package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.namingserver.domain.NamingServer;

import java.io.IOException;

public class NamingServerMain {

    public static void main(String[] args) {

        NamingServer namingServer = new NamingServer();

        final BindableService namingServerServiceImpl = new NamingServerServiceImpl(namingServer);

        Server server = ServerBuilder.forPort(5001)
                .addService(namingServerServiceImpl)
                .build();
        try {
            server.start();
            System.out.println("Naming server started");
            System.out.println("Press enter to shutdown");
            System.in.read();
            server.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }
    }

}
