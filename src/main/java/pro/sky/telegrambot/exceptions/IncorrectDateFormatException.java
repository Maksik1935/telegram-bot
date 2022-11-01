package pro.sky.telegrambot.exceptions;

public class IncorrectDateFormatException extends RuntimeException{
    public IncorrectDateFormatException() {
        super("Невозможно распознать дату");
    }
}
