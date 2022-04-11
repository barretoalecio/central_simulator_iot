package central_simulador_iot.br.ucsal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import central_simulador_iot.br.ucsal.models.MessageContentModel;

public class Server {

	public static final int PORT = 3000;
	public static final String ADDRESS = "127.0.0.1";

	private final Selector selector;
	private final ServerSocketChannel serverChannel;

	private final ByteBuffer buffer;

	private List<Integer> trucksFreeCode = new LinkedList<Integer>();
	private List<Integer> containersCode = new LinkedList<Integer>();

	public static void main(String[] args) {
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");

		try {
			Server server = new Server();
			server.start();
		} catch (IOException e) {
			System.err.println("Erro durante execução do servidor: " + e.getMessage());
		}
	}

	public Server() throws IOException {
		buffer = ByteBuffer.allocate(1024);
		selector = Selector.open();
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		serverChannel.bind(new InetSocketAddress(ADDRESS, PORT), 10000);
		System.out.println("Servidor iniciado ( " + ADDRESS + ":" + PORT + " )");
	}

	public void start() {
		while (true) {
			try {
				selector.select();
				processEvents(selector.selectedKeys());
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private void processEvents(Set<SelectionKey> selectionKeys) {
		selectionKeys.stream().parallel().forEach(this::processEvent);
		selectionKeys.clear();
	}

	private void processEvent(SelectionKey selectionKey) {
		if (!selectionKey.isValid()) {
			return;
		}

		try {
			processConnectionAccept(selectionKey, selector);
			processRead(selectionKey);
		} catch (IOException ex) {
			System.out.println("Erro ao processar evento: " + ex.getMessage());
		}
	}

	private void processConnectionAccept(SelectionKey key, Selector selector) throws IOException {
		if (!key.isAcceptable()) {
			return;
		}
		SocketChannel clientChannel = serverChannel.accept();
		System.out.println("Cliente " + clientChannel.getRemoteAddress() + " conectado.");
		clientChannel.configureBlocking(false);
		clientChannel.register(selector, SelectionKey.OP_READ);
	}

	private void processRead(SelectionKey selectionKey) throws IOException {
		if (!selectionKey.isReadable()) {
			return;
		}

		SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
		buffer.clear();

		int bytesRead;
		try {
			bytesRead = clientChannel.read(buffer);
		} catch (IOException e) {
			System.err.println(
					"Conexão fechada pelo cliente " + clientChannel.getRemoteAddress() + ": " + e.getMessage());
			clientChannel.close();
			selectionKey.cancel();
			return;
		}

		if (bytesRead <= 0) {
			return;
		}

		buffer.flip();
		byte[] data = new byte[bytesRead];
		buffer.get(data);
		String message = new String(data);
		System.out.println("Mensagem recebida do cliente " + clientChannel.getRemoteAddress() + ": " + message + " ("
				+ bytesRead + " bytes lidos)");

		MessageContentModel messageContent = new MessageContentModel(message);

		if (messageContent.getMessage().equals("LIVRE")) {
			recivedFreeFromTruck(messageContent.getCode());
			// TODO(barretoalecio): Refatorar isso!
			clientChannel.write(ByteBuffer.wrap("COLETAR 6\n".getBytes()));
		}
		if (messageContent.getMessage().equals("CHEIO")) {
			recivedFullFromContainer(messageContent.getCode());
		}
		if (messageContent.getMessage().equals("CHEGUEI_CONTAINER")) {
			truckWasArrivedContainer(messageContent.getCode());
		}
		if (messageContent.getMessage().equals("COLETA_FINALIZADA")) {
			recivedFinishTravelFromTruck(messageContent.getCode());
		}
	}

	private void recivedFreeFromTruck(Integer code) {
		System.out.println("Recebido caminhao Livre: " + code);
		trucksFreeCode.add(code);
	}

	private void recivedFullFromContainer(Integer code) {
		System.out.println("Recebido container Cheio: " + code);
		containersCode.add(code);
	}

	private void truckWasArrivedContainer(Integer code) {
		System.out.println("Caminhao chegou ate o Container: " + code);
	}

	private void recivedFinishTravelFromTruck(Integer code) {
		System.out.println("Caminhao finalizou a Viagem: " + code);
	}

}