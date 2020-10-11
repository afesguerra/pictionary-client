package com.afesguerra.pictionary.client.gui;

import com.afesguerra.pictionary.client.Hilo;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Log4j2
public class GameStartFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JTextField serverAddressField; // Campo donde se ingresa la dirección IP del
    private final JTextField usernameField; // Campo donde se ingresa el nombre de usuario
    private final DatagramSocket sck;

    public GameStartFrame(final DatagramSocket sck) {
        this.sck = sck;
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

            final DatagramPacket outbound = new DatagramPacket(
                    nombre.getBytes(),
                    nombre.getBytes().length,
                    InetAddress.getByName(serverAddressField.getText()),
                    1337
            );
            sck.send(outbound);

            byte[] a = new byte[10000];
            DatagramPacket p = new DatagramPacket(a, a.length);
            sck.receive(p);

            String[] m = (new String(p.getData(), 0, p
                    .getLength())).split(";");
            if (m[0].equals("error")) {
                JOptionPane.showMessageDialog(this, m[1],
                        "ERROR, nombre en uso",
                        JOptionPane.PLAIN_MESSAGE);
                System.exit(0);
            } else {
                final InetAddress ip = p.getAddress();
                final int puerto = p.getPort();
                this.setVisible(false);
                final boolean dibujante = m[0].equals(nombre);

                final String word = m[1];
                final GameFrame gameFrame = new GameFrame(dibujante, sck, ip, puerto, nombre, word);
                gameFrame.setTitle("Sketching: " + nombre);
                gameFrame.setVisible(true);
                new Hilo(gameFrame, sck).start();
            }
        } catch (IOException e) {
            log.error(e);
        }
    }
}
