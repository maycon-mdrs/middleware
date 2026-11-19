package br.imd.ufrn.app.instance;

import br.imd.ufrn.app.gateway.HeartbeatClient;
import br.imd.ufrn.middleware.Middleware;
import br.imd.ufrn.middleware.extension.interceptor.LoggingInterceptor;
import br.imd.ufrn.middleware.extension.protocol.http.HttpProtocolPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Processo de instância BR/PT usando o middleware.
 *
 * Uso: InstanceApp &lt;br|pt&gt; &lt;id&gt; &lt;port&gt; [gatewayHost] [advertiseHost]
 */
public final class InstanceApp {

    private InstanceApp() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: InstanceApp <br|pt> <id> <port> [gatewayHost] [advertiseHost]");
            return;
        }

        String zone = args[0].trim().toLowerCase();
        String id = args[1].trim();
        int port = Integer.parseInt(args[2]);
        String gatewayHost = args.length > 3 ? args[3] : "127.0.0.1";
        String advertiseHost = args.length > 4 ? args[4] : "127.0.0.1";

        writePidFile(id);

        ZoneTimeService service = new ZoneTimeService(zone);

        Middleware mw = new Middleware();
        mw.register(service);
        // Sem log por request na instância: sob JMeter o System.out sincronizado
        // engolia threads (mesmo sintoma do TP1 ~20–30s). Gateway já amostra logs.
        if (Boolean.parseBoolean(System.getProperty("mw.instance.log", "false"))) {
            mw.interceptor(new LoggingInterceptor());
        }
        mw.protocol(new HttpProtocolPlugin(port));
        mw.start();

        HeartbeatClient heartbeat = new HeartbeatClient(
                gatewayHost, 9000, zone, advertiseHost, port, id);
        heartbeat.start();

        System.out.println("[instance] " + id + " zone=" + zone + " http=" + port);
        Thread.currentThread().join();
    }

    private static void writePidFile(String instanceId) {
        try {
            Path dir = Path.of("logs");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(instanceId + ".pid"),
                    String.valueOf(ProcessHandle.current().pid()));
        } catch (IOException e) {
            System.err.println("[instance] falha ao gravar pid: " + e.getMessage());
        }
    }
}
