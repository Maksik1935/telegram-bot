package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.exceptions.IncorrectDateFormatException;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository repository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }
    public int flag = 0;

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
                if(update.message().text().equalsIgnoreCase("/start")) {
                    getStart(update);
                } else {
                    createTask(update);
                }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void getStart(Update update) {
        logger.info("Bot is starting");
        String message;
        if(flag == 0) {
            message = "Привет. Мы патриоты, поэтому презираем анлийский. Пиши на русском. " +
                    "Установить напомналку можно в формате ДД.ММ.ГГГГ 00:00 Украсть коня у цыган";
            flag = 1;
        } else {
            message = "Приложение уже запущено. Введи напоминалку в формате ДД.ММ.ГГГГ 00:00 Перегнать трамвай в Турцию";
        }
        telegramBot.execute(new SendMessage(update.message().chat().id(), message));
    }

    private void createTask(Update update) {
        logger.info("Creating task");
        Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+|\\w]+)");
        String userMessage = update.message().text();
        Matcher matcher = pattern.matcher(userMessage);
        NotificationTask task = new NotificationTask();
        if (matcher.matches()) {
            try {
                LocalDateTime time = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                task.setTime(time.truncatedTo(ChronoUnit.MINUTES));
            } catch (Exception e) {
                throw new IncorrectDateFormatException();
            }
            task.setMessage(matcher.group(2));
            task.setChat_id(update.message().chat().id());
            repository.save(task);
            telegramBot.execute(new SendMessage(task.getChat_id(), "Напоминание установлено"));
        }
    }

        @Scheduled(cron = "0 0/1 * * * *")
        public void sendNotification() {
            logger.info("Check time and notifications");
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            List<NotificationTask> notificationToSendList = repository.findAllByTime(now);
            for(NotificationTask task : notificationToSendList) {
                telegramBot.execute(new SendMessage(task.getChat_id(), task.getMessage()));
                logger.info("Notification was send");
            }
        }
    }

