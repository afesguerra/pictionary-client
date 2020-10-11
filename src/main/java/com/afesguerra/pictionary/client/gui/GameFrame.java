package com.afesguerra.pictionary.client.gui;

import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

@Log4j2
public class GameFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JPanel canvas; // Panel en el cual se dibuja
    private final JTextField chatInput; // Campo donde se escriben palabras a adivinar
    private final JTextArea chatbox; // Campo donde aparecen las palabras enviadas
    private final DatagramSocket socket;
    private final InetAddress ip;
    private final int port;
    private final String nombre;

    /**
     * Crea un objeto de la clase VentanaJuego
     */
    public GameFrame(final boolean drawer, final DatagramSocket socket, final InetAddress ip, final int port, final String nombre, final String word) {
        this.socket = socket;
        this.ip = ip;
        this.port = port;
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
            canvas.addMouseWheelListener(this::changeColor);
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
    }

    public void cambiarColor(Color c) {
        Graphics g = canvas.getGraphics();
        g.setColor(c);
        g.fillRect(10, 20, 20, 20);
    }

    public void dibujar(int x, int y) {
        Graphics g = canvas.getGraphics();
        g.fillOval(x - 5, y - 5, 10, 10);
    }

    public void escribir(String a) {
        if (chatbox.getText().equals(""))
            chatbox.setText(a);
        else
            chatbox.setText(chatbox.getText() + "\n" + a);
    }

    private void changeColor(MouseWheelEvent event) {
        final Random random = new Random();
        final String payload = "4;" + random.nextInt(256) + ";" + random.nextInt(256) + ";" + random.nextInt(256);
        DatagramPacket pck = new DatagramPacket(payload.getBytes(), payload.getBytes().length, ip, port);
        try {
            socket.send(pck);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void sendWordListener(ActionEvent event) {
        try {
            if (!chatInput.getText().equalsIgnoreCase("")) {
                String a = chatInput.getText();
                String p = "2;" + nombre + ";" + a.replace(';', ' ');
                DatagramPacket pck = new DatagramPacket(p.getBytes(),
                        p.getBytes().length, ip, port);
                socket.send(pck);
                chatInput.setText("");
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    private class DrawActionListener extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent event) {
            try {
                String p = String.join(";", "1", String.valueOf(event.getX()), String.valueOf(event.getY()));
                DatagramPacket pck = new DatagramPacket(p.getBytes(), p.getBytes().length, ip, port);
                socket.send(pck);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    private class CloseWindowListener extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            try {
                String p = "3;" + nombre;
                DatagramPacket pck = new DatagramPacket(p.getBytes(), p.getBytes().length, ip, port);
                socket.send(pck);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }
}
