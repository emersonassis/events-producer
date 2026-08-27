package br.com.outbox.config.message;

/**
 * Leitura das configurações de conexão do RabbitMQ: host, porta, usuário e senha.
 */
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class RabbitMQConfig {

    public String getHost() {
        return ConfigProvider.getConfig().getValue("rabbitmq.host", String.class);
    }

    public int getPort() {
        return ConfigProvider.getConfig().getValue("rabbitmq.port", Integer.class);
    }

    public String getUsername() {
        return ConfigProvider.getConfig().getValue("rabbitmq.username", String.class);
    }

    public String getPassword() {
        return ConfigProvider.getConfig().getValue("rabbitmq.password", String.class);
    }

    public int getReconnectAttempts() {
        return ConfigProvider.getConfig().getValue("rabbitmq.reconnect-attempts", Integer.class);
    }
}
