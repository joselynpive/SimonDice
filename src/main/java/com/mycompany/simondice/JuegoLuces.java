/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.simondice;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author alumnog
 */
public class JuegoLuces extends JFrame {

    /** Botones que representan las luces del tablero actual. */
    private JButton[] botones;

    /** Botón para iniciar o volver a iniciar una partida. */
    private JButton btnIniciar;

    /** Botones para seleccionar el tamaño del tablero. */
    private JButton btn2x2;
    private JButton btn3x3;
    private JButton btn5x5;

    /** Etiqueta que muestra la puntuación. */
    private JLabel lblPuntaje;

    /** Panel que contiene las luces. */
    private JPanel panelTablero;

    /** Panel inferior con la puntuación y los controles. */
    private JPanel panelControl;

    /** Secuencia de índices que el jugador debe memorizar. */
    private final List<Integer> secuencia;

    /** Posición de la secuencia que debe pulsar el usuario. */
    private int pasoUsuario;

    /** Indica si el juego está esperando la respuesta del jugador. */
    private volatile boolean escuchandoUsuario;

    /** Indica el tamaño actual del tablero (2, 3 o 5). */
    private int tamanoActual;

    /** Colores apagados y encendidos generados para el tablero actual. */
    private Color[] coloresApagados;
    private Color[] coloresEncendidos;

    /** Generador aleatorio para elegir las luces de la secuencia. */
    private final Random random;

    /**
     * Construye la ventana y crea inicialmente un tablero de 2x2.
     */
    public JuegoLuces() {
        setTitle("Juego de Secuencia de luces");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);
        setMinimumSize(new Dimension(400, 450));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        secuencia = new ArrayList<>();
        random = new Random();
        escuchandoUsuario = false;
        tamanoActual = 2;

        panelTablero = new JPanel();
        panelControl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        lblPuntaje = new JLabel("Puntaje: 0");
        btnIniciar = new JButton("Iniciar Juego");
        btn2x2 = new JButton("2x2");
        btn3x3 = new JButton("3x3");
        btn5x5 = new JButton("5x5");

        btnIniciar.addActionListener(e -> comenzarJuego());
        btn2x2.addActionListener(e -> cambiarTamano(2));
        btn3x3.addActionListener(e -> cambiarTamano(3));
        btn5x5.addActionListener(e -> cambiarTamano(5));

        panelControl.add(lblPuntaje);
        panelControl.add(btnIniciar);
        panelControl.add(btn2x2);
        panelControl.add(btn3x3);
        panelControl.add(btn5x5);

        add(panelTablero, BorderLayout.CENTER);
        add(panelControl, BorderLayout.SOUTH);

        reconstruirTablero(2);
    }

    /**
     * Inicia una partida nueva y prepara la primera ronda.
     */
    private void comenzarJuego() {
        secuencia.clear();
        pasoUsuario = 0;
        escuchandoUsuario = false;
        lblPuntaje.setText("Puntaje: 0");
        btnIniciar.setEnabled(false);
        siguienteRonda();
    }

    /**
     * Agrega un paso aleatorio a la secuencia y reproduce la ronda.
     */
    private void siguienteRonda() {
        pasoUsuario = 0;
        escuchandoUsuario = false;

        // El límite depende de la cantidad de luces del tablero actual.
        secuencia.add(random.nextInt(botones.length));

        lblPuntaje.setText("Puntaje: " + (secuencia.size() - 1));
        reproducirSecuenciaConHilo();
    }

    /**
     * Reproduce la secuencia en un hilo secundario para no congelar la
     * interfaz. Las modificaciones visuales se ejecutan en el hilo de Swing.
     */
    private void reproducirSecuenciaConHilo() {
        establecerBotonesTamanoHabilitados(false);

        // Copia estable de la secuencia que se va a reproducir.
        final int[] secuenciaActual = secuencia.stream()
                .mapToInt(Integer::intValue)
                .toArray();

        Thread hiloAnimacion = new Thread(() -> {
            try {
                Thread.sleep(700);

                for (int botonIndex : secuenciaActual) {
                    ejecutarEnSwing(() ->
                            botones[botonIndex].setBackground(
                                    coloresEncendidos[botonIndex]));

                    Thread.sleep(450);

                    ejecutarEnSwing(() ->
                            botones[botonIndex].setBackground(
                                    coloresApagados[botonIndex]));

                    Thread.sleep(180);
                }

                SwingUtilities.invokeLater(() -> {
                    escuchandoUsuario = true;
                    establecerBotonesTamanoHabilitados(true);
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(() -> {
                    escuchandoUsuario = false;
                    establecerBotonesTamanoHabilitados(true);
                });
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        establecerBotonesTamanoHabilitados(true));
            }
        }, "HiloAnimacionLuces");

        hiloAnimacion.start();
    }

    /**
     * Ejecuta una modificación de interfaz en el Event Dispatch Thread
     * y espera a que termine para mantener el orden de la animación.
     *
     * @param accion operación visual que se ejecutará en Swing
     * @throws InterruptedException si se interrumpe el hilo de animación
     * @throws InvocationTargetException si ocurre un error en la operación
     */
    private void ejecutarEnSwing(Runnable accion)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(accion);
    }

    /**
     * Comprueba si el botón pulsado coincide con el siguiente elemento
     * esperado de la secuencia.
     *
     * @param botonPresionado índice de la luz que pulsó el jugador
     */
    private void verificarClickUsuario(int botonPresionado) {
        if (botonPresionado == secuencia.get(pasoUsuario)) {
            destelloRapido(botonPresionado);
            pasoUsuario++;

            if (pasoUsuario == secuencia.size()) {
                siguienteRonda();
            }
        } else {
            escuchandoUsuario = false;
            JOptionPane.showMessageDialog(
                    this,
                    "¡Te equivocaste! Fin del juego.",
                    "Game Over",
                    JOptionPane.ERROR_MESSAGE
            );
            btnIniciar.setEnabled(true);
            establecerBotonesTamanoHabilitados(true);
        }
    }

    /**
     * Hace parpadear brevemente una luz cuando el jugador la selecciona.
     * Se utiliza un Timer de Swing para actualizar la interfaz en su hilo.
     *
     * @param index índice del botón que debe parpadear
     */
    private void destelloRapido(int index) {
        botones[index].setBackground(coloresEncendidos[index]);

        Timer timer = new Timer(150, (ActionEvent e) ->
                botones[index].setBackground(coloresApagados[index]));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Cambia el tamaño del tablero y reinicia la partida y la puntuación.
     *
     * @param nuevoTamano número de filas y columnas del nuevo tablero
     */
    private void cambiarTamano(int nuevoTamano) {
        if (nuevoTamano != 2 && nuevoTamano != 3 && nuevoTamano != 5) {
            return;
        }

        // Los botones de tamaño están deshabilitados durante la animación.
        if (!btn2x2.isEnabled()) {
            return;
        }

        secuencia.clear();
        pasoUsuario = 0;
        escuchandoUsuario = false;
        btnIniciar.setEnabled(true);
        lblPuntaje.setText("Puntaje: 0");

        reconstruirTablero(nuevoTamano);
    }

    /**
     * Elimina las luces anteriores y crea un GridLayout de n por n.
     * También genera un par de colores (apagado/encendido) para cada luz.
     *
     * @param nuevoTamano cantidad de filas y columnas
     */
    private void reconstruirTablero(int nuevoTamano) {
        tamanoActual = nuevoTamano;
        int cantidadLuces = tamanoActual * tamanoActual;

        coloresApagados = new Color[cantidadLuces];
        coloresEncendidos = new Color[cantidadLuces];
        botones = new JButton[cantidadLuces];

        panelTablero.removeAll();
        panelTablero.setLayout(new GridLayout(
                tamanoActual, tamanoActual, 10, 10
        ));

        for (int i = 0; i < cantidadLuces; i++) {
            // Distribuye los tonos alrededor del círculo de color.
            float tono = (float) i / cantidadLuces;
            coloresApagados[i] = Color.getHSBColor(tono, 0.95f, 0.35f);
            coloresEncendidos[i] = Color.getHSBColor(tono, 1.0f, 1.0f);

            final int index = i;
            botones[i] = new JButton();
            botones[i].setBackground(coloresApagados[i]);
            botones[i].setOpaque(true);
            botones[i].setBorderPainted(false);
            botones[i].setFocusPainted(false);

            botones[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (escuchandoUsuario) {
                        verificarClickUsuario(index);
                    }
                }
            });

            panelTablero.add(botones[i]);
        }

        panelTablero.revalidate();
        panelTablero.repaint();
    }

    /**
     * Habilita o deshabilita los botones para cambiar el tamaño del tablero.
     *
     * @param habilitados true para habilitarlos; false para deshabilitarlos
     */
    private void establecerBotonesTamanoHabilitados(boolean habilitados) {
        btn2x2.setEnabled(habilitados);
        btn3x3.setEnabled(habilitados);
        btn5x5.setEnabled(habilitados);
    }

    /**
     * Punto de entrada de la aplicación. Crea la ventana en el hilo de Swing.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JuegoLuces juego = new JuegoLuces();
            juego.setVisible(true);
        });
    }
}
