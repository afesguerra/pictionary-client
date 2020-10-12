package com.afesguerra.pictionary.client.gui;

import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Random;

@Log4j2
public class GameFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JPanel canvas; // Panel en el cual se dibuja
    private final JTextField chatInput; // Campo donde se escriben palabras a adivinar
    private final JTextArea chatbox; // Campo donde aparecen las palabras enviadas
    private final DatagramChannel channel;
    private final SocketAddress serverAddress;
    private final String nombre;
    private final ChannelWorker worker;

    /**
     * Crea un objeto de la clase VentanaJuego
     */
    public GameFrame(final boolean drawer, final DatagramChannel channel, final SocketAddress serverAddress, final String nombre, final String word) {
        this.channel = channel;
        this.serverAddress = serverAddress;
        this.nombre = nombre;
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new GridLayout(1, 2));
        // Panel donde aparecen todas las palabras
        JPanel panelExtra = new JPanel();
        panelExtra.setLayout(new FlowLayout());

        chatbox = new JTextArea("");
        chatbox.setEditable(false);
        chatbox.setBackground(this.getBackground());

        // Panel deslizable para visualizar todas las palabras
        JScrollPane scroll = new JScrollPane(chatbox);
        scroll.setBorder(BorderFactory.createTitledBorder("Pistas"));
        scroll.setPreferredSize(new Dimension(350, 400));

        chatInput = new JTextField();
        chatInput.setPreferredSize(new Dimension(350, 40));
        chatInput.setBorder(BorderFactory.createTitledBorder("Ingrese la palabra"));
        chatInput.setBackground(this.getBackground());
        chatInput.setFocusable(true);

        // Boton que permite enviar los datos
        JButton boton = new JButton("Enviar");

        panelExtra.add(scroll);
        panelExtra.add(chatInput);
        panelExtra.add(boton);

        panelExtra.setBorder(BorderFactory.createTitledBorder("Palabras"));
        add(panelExtra);

        canvas = new JPanel();
        canvas.setBorder(BorderFactory.createTitledBorder("Dibujo"));
        add(canvas);

        this.addWindowListener(new CloseWindowListener());

        if (drawer) {
            canvas.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            canvas.addMouseWheelListener(this::changeColorListener);
            canvas.addMouseMotionListener(new DrawActionListener());

            JOptionPane.showMessageDialog(
                    this,
                    String.format("Tú eres el dibujante, la palabra a dibujar es: %s", word),
                    "Tú eres el dibujante",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            boton.addActionListener(this::sendWordListener);
            chatInput.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == '\n') {
                        sendWordListener(null);
                    }
                }
            });
            JOptionPane.showMessageDialog(this,
                    "Iniciando juego",
                    "A jugar",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        worker = new ChannelWorker();
        worker.execute();
    }

    // region listeners

    private void changeColorListener(MouseWheelEvent event) {
        final Random random = new Random();
        final String payload = "4;" + random.nextInt(256) + ";" + random.nextInt(256) + ";" + random.nextInt(256);
        sendMessage(payload);
    }

    private void sendWordListener(ActionEvent event) {
        if (!chatInput.getText().equalsIgnoreCase("")) {
            String writtenWord = chatInput.getText();
            String message = "2;" + nombre + ";" + writtenWord.replace(';', ' ');
            sendMessage(message);
            chatInput.setText("");
        }
    }

    private class DrawActionListener extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent event) {
            String p = String.join(";", "1", String.valueOf(event.getX()), String.valueOf(event.getY()));
            sendMessage(p);
        }
    }

    private class CloseWindowListener extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            String p = "3;" + nombre;
            sendMessage(p);
        }
    }

    // endregion

    private void sendMessage(final String message) {
        final ByteBuffer bb = ByteBuffer.wrap(message.getBytes());
        try {
            channel.send(bb, serverAddress);
        } catch (IOException e) {
            log.error("Error sending message to server", e);
        }
    }

    private class ChannelWorker extends SwingWorker<Void, Void> {
        private static final String MESSAGE_SEPARATOR = ";";

        @Override
        protected Void doInBackground() throws Exception {
            while (!isCancelled()) {
                final ByteBuffer bb = ByteBuffer.allocate(10000);
                channel.receive(bb);

                bb.flip();
                int limits = bb.limit();
                byte[] bytes = new byte[limits];
                bb.get(bytes, 0, limits);
                final String message = new String(bytes);

                log.trace(message);

                parseMessage(message);
            }
            return null;
        }

        private void parseMessage(final String message) {
            String[] msj = message.split(MESSAGE_SEPARATOR);

            switch (Integer.parseInt(msj[0])) {
                case 0:
                    // Alguien ganó
                    JOptionPane.showMessageDialog(
                            GameFrame.this,
                            msj[2],
                            "¡¡GANADOR!!",
                            JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                    break;
                case 1:
                    // coordenadas
                    int x = Integer.parseInt(msj[1]);
                    int y = Integer.parseInt(msj[2]);
                    dibujar(x, y);
                    break;
                case 2:
                    // palabra+
                    escribir((msj[1] + ": " + msj[2]));
                    break;
                case 3:
                    // alguien se desconecto
                    JOptionPane.showMessageDialog(GameFrame.this, msj[2], "Error de conexión", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                    break;
                case 4:
                    final Color color = new Color(Integer.parseInt(msj[1]), Integer.parseInt(msj[2]), Integer.parseInt(msj[3]));
                    cambiarColor(color);
                    break;
                default:
                    log.warn("Received unexpected message: {}", message);
                    break;
            }
        }

        public void dibujar(int x, int y) {
            Graphics g = canvas.getGraphics();
            g.fillOval(x - 5, y - 5, 10, 10);
        }

        private void escribir(String a) {
            if (chatbox.getText().equals("")) {
                chatbox.setText(a);
            } else {
                chatbox.setText(chatbox.getText() + "\n" + a);
            }
        }

        public void cambiarColor(Color c) {
            Graphics g = canvas.getGraphics();
            g.setColor(c);
            g.fillRect(10, 20, 20, 20);
        }

    }
}
