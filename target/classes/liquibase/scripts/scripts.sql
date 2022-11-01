-- liquibase formatted sql

-- changeset mkachalov:1
create table notifications
(
    chat_id integer   not null,
    id      integer
        constraint id
            primary key,
    time    timestamp not null,
    message text      not null
);