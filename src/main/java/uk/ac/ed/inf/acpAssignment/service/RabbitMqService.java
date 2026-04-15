package uk.ac.ed.inf.acpAssignment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

@Slf4j
@Service
public class RabbitMqService {
  private static final String HOST = "localhost"; // change later to env var
  private static final String STUDENT_ID = "your-student-id";

  public void sendMessages(String queueName, int messageCount) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(HOST);

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {

      channel.queueDeclare(queueName, true, false, false, null);

      for (int i = 0; i < messageCount; i++) {
        String message = String.format(
            "{\"uid\":\"%s\",\"counter\":%d}",
            STUDENT_ID,
            i
        );

        channel.basicPublish(
            "",
            queueName,
            null,
            message.getBytes()
        );

        log.info("Sent message: {}", message);
      }

    } catch (Exception e) {
      log.error("Error sending messages to RabbitMQ", e);
    }
  }
}
