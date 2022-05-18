// client.c
#include <arpa/inet.h>
#include <ctype.h>
#include <errno.h>
#include <inttypes.h>
#include <netdb.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#include "protocol.h"
#include "util/list.h"

struct server_data
{
    int serv_sock;
    pthread_t reply_thread;
    pthread_t stdin_thread;
};

void safe_print_out(const void *buf)
{
    if (printf("%s", (const char *)buf) < 0)
    {
        fprintf(stderr, "Error writing to stdout.\n");
        exit(1);
    }
}

void print_prompt()
{
    safe_print_out("> ");
    if (fflush(stdout) != 0)
    {
        perror("fflush");
        exit(1);
    }
}

void *stdin_handler(void *data)
{
    pthread_setcancelstate(PTHREAD_CANCEL_ENABLE, NULL);
    pthread_setcanceltype(PTHREAD_CANCEL_ASYNCHRONOUS, NULL);
    struct server_data *sd = (struct server_data *)data;
    while (1)
    {
        print_prompt();
        char input[MAX_BUF_SIZE];
        ssize_t read_ret_value = read(STDIN_FILENO, input, MAX_BUF_SIZE);
        if (read_ret_value < 0)
        {
            perror("read");
            goto stdin_exit;
        }
        else if (read_ret_value == 0)
        {
            goto stdin_exit;
        }

        input[read_ret_value - 1] = '\0';
        if (input[0] == 'q' && input[1] == '\0')
        {
            goto stdin_exit;
        }
        else if (isdigit(input[0]) && (input[1] == '\0'))
        {
            //send message
            printf("Waiting for an announce...\n");
            int station_number = atoi(&input[0]);
            send_command_message(sd->serv_sock, MESSAGE_TYPE_SET_STATION, station_number);
        }
        else
        {
            printf("Input must be a 'q' or a valid station number\n");
        }
    }

stdin_exit:
    close(sd->serv_sock);
    pthread_cancel(sd->reply_thread);
    printf("cancellation sent\n");
    pthread_exit(0);
}

void free_songname(void *data)
{
    free(data);
}

void *reply_handler(void *data)
{
    struct server_data *sd = (struct server_data *)data;
    //receive reply
    uint8_t replyType;
    uint8_t songnameSize;
    char *songname = NULL;
    pthread_cleanup_push(&free_songname, songname);
    int err;
    while (1)
    {
        if ((err = recv_reply_message(sd->serv_sock, &replyType, &songnameSize, &songname)) < 0)
        {
            if (err == -2)
            {
                printf("Connection timeout. Terminating connection.");
            }
            goto message_exit;
        }
        if (replyType == 0)
        {
            printf("Second Welcome received; shutting down connection\n");
            break;
        }
        else if (replyType == 1)
        {
            printf("New song announced: %.*s\n", songnameSize, songname);
            free(songname);
        }
        else if (replyType == 2)
        {
            printf("INVALID_COMMAND_REPLY: %.*s", songnameSize, songname);
            printf("Server has closed the connection\n");
            goto message_exit;
        }
        else
        {
            printf("Received invalid reply; shutting down connection.\n");
            break;
        }
    }

message_exit:
    close(sd->serv_sock);
    pthread_cleanup_pop(1);
    pthread_cancel(sd->stdin_thread);
    pthread_exit(0);
}

int main(int argc, char **argv)
{
    if (argc != 4)
    {
        printf("Usage: snowcast_control <servername> <serverport> <udpport>\n");
        exit(1);
    }

    int rv;
    struct addrinfo hints, *res, *servinfo;
    struct server_data sd;

    char *server_addr = argv[1];
    char *server_port = argv[2];
    char *udpport_str = argv[3];
    uint16_t udpport = (uint16_t)atoi(udpport_str);

    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;     // IPv4, IPv6 socket
    hints.ai_socktype = SOCK_STREAM; //TCP socket

    if ((rv = getaddrinfo(server_addr, server_port, &hints, &servinfo)) != 0)
    {
        perror(gai_strerror(rv));
        exit(1);
    }

    for (res = servinfo; res != NULL; res = res->ai_next)
    {
        if ((sd.serv_sock = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) < 0)
        {
            continue;
        }
        if (connect(sd.serv_sock, res->ai_addr, res->ai_addrlen) < 0)
        {
            close(sd.serv_sock);
            continue;
        }
        break;
    }

    if (res == NULL)
    {
        printf("Failed to connect\n");
        exit(1);
    }

    char servAddrStr[INET_ADDRSTRLEN];
    inet_ntop(AF_INET, servinfo->ai_addr, servAddrStr, INET_ADDRSTRLEN);
    printf("%s -> %s\n", server_addr, servAddrStr);
    printf("Type in a number to set the station we're listening to to that number.\n");
    printf("Type in 'q' or press CTRL+C to quit.\n");

    if (send_command_message(sd.serv_sock, MESSAGE_TYPE_HELLO, udpport) < 0)
    {
        fprintf(stderr, "send_command_message failed.\n");
        return -1;
    }

    struct command_message msg;
    msg.commandType = 1;
    while (msg.commandType != 0)
    {
        if (recv_command_message(sd.serv_sock, &msg) < 0)
        {
            fprintf(stderr, "Receiving welcome  message failed.\n");
            exit(1);
        }
    }
    printf("> The server has  %d stations.\n", msg.numberStations);
    pthread_t reply_thread, stdin_thread;
    pthread_create(&reply_thread, NULL, reply_handler, &sd);
    pthread_create(&stdin_thread, NULL, stdin_handler, &sd);

    pthread_join(reply_thread, NULL);
    pthread_join(stdin_thread, NULL);

    freeaddrinfo(res);

    return 0;
}
