package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import pt.tecnico.distledger.server.domain.ServerState;

public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        // receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments, na outras entregas tem de ser 2
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}
		
		final int port = Integer.parseInt(args[0]);
		final String qualifier = args[1];


		ServerState st = new ServerState("localhost:" + port, qualifier);
		final BindableService userImpl = new UserServiceImpl(st);
		final BindableService adminImpl = new AdminServiceImpl(st);
		final BindableService crossImpl = new DistLedgerCrossServerServiceImpl(st);

		// Create the secondary server
		Server server = ServerBuilder.forPort(port)
								.addService(userImpl)
								.addService(adminImpl)
								.addService(crossImpl)
								.build();

		server.start();

		System.out.println("Server started");

		System.out.println("Press enter to shutdown");
		System.in.read();
		st.getCrossService().delete("localhost:" + port);
		
		System.exit(0);

    }

}
