package com.afesguerra.pictionary.client.gui;

import com.afesguerra.pictionary.client.Hilo;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

@Log4j2
public class GameStartFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JTextField serverAddressField; // Campo donde se ingresa la dirección IP del
    private final JTextField usernameField; // Campo donde se ingresa el nombre de usuario
    private final DatagramChannel channel;

    public GameStartFrame(final DatagramChannel channel) {
        this.channel = channel;
        setTitle("Sketching");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new GridLayout(3, 1));

        serverAddressField = new JTextField();
        serverAddressField.setBorder(BorderFactory.createTitledBorder("Ingrese la IP del servidor"));

        usernameField = new JTextField();
        usernameField.setBorder(BorderFactory.createTitledBorder("Ingrese su nombre de usuario"));

        JButton startGameButton = new JButton("Enviar");
        startGameButton.addActionListener(this::startGame);

        add(serverAddressField);
        add(usernameField);
        add(startGameButton);
    }


    private void startGame(ActionEvent event) {
        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            if (usernameField.getText().equals("") || serverAddressField.getText().equals("")) {
                JOptionPane.showMessageDialog(this,
                        "Datos incorrectos\nAplicación Finalizada",
                        "Error", JOptionPane.INFORMATION_MESSAGE);
                throw new RuntimeException("Unexpected error");
            }

            String palabra = usernameField.getText();
            final String nombre = palabra.replaceAll(";", " ");


            final InetSocketAddress serverAddress = new InetSocketAddress(serverAddressField.getText(), 1337);
            sendMessage(nombre, serverAddress);

            final ByteBuffer bb = ByteBuffer.allocate(10000);
            final SocketAddress newAddress = channel.receive(bb);

            bb.flip();
            int limits = bb.limit();
            byte[] bytes = new byte[limits];
            bb.get(bytes, 0, limits);
            final String message = new String(bytes);
            String[] m = message.split(";");

            if (m[0].equals("error")) {
                JOptionPane.showMessageDialog(this, m[1],
                        "ERROR, nombre en uso",
                        JOptionPane.PLAIN_MESSAGE);
                return;
            }

            this.setVisible(false);

            final boolean dibujante = m[0].equals(nombre);
            final String word = m[1];

            final GameFrame gameFrame = new GameFrame(dibujante, channel, newAddress, nombre, word);
            gameFrame.setTitle("Sketching: " + nombre);
            gameFrame.setVisible(true);
            new Hilo(gameFrame, channel).start();

        } catch (IOException e) {
            log.error(e);
        }
    }

    private void sendMessage(final String message, final SocketAddress serverAddress) {
        final ByteBuffer bb = ByteBuffer.wrap(message.getBytes());
        try {
            channel.send(bb, serverAddress);
        } catch (IOException e) {
            log.error("Error sending message to server", e);
        }
    }
}
