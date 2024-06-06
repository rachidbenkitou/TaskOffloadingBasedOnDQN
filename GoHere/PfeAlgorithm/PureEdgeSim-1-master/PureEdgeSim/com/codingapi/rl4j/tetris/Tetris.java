package com.codingapi.rl4j.tetris;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class Tetris extends JFrame {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    JLabel statusbar, score;

    private Board board = null;

    public void left() {
        board.left();
    }

    public void right() {
        board.right();
    }

    public void down() {
        board.down();
    }

    public void up() {
        board.up();
    }

    public void dropDown() {
        board.dropDown();
    }

    public Tetris() {
        score = new JLabel("Score:");
        statusbar = new JLabel(" 0");
        add(score, BorderLayout.SOUTH);
        add(statusbar, BorderLayout.SOUTH);
        board = new Board(this);
        add(board);
        board.start();

        setSize(200, 400);
        setTitle("Tetris");
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                int confirmed = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to exit the program?", "Exit Program Message Box",
                        JOptionPane.YES_NO_OPTION);

                if (confirmed == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });
    }

    public JLabel getStatusBar() {
        return statusbar;
    }


    public void start() {
        board.start();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    public void close() {
        this.setVisible(false);
    }

    public double[] toArray() {
        return board.toArray();
    }

    public double getScore() {
        return board.getScore();
    }

    public boolean isOver() {
        return board.isOver();
    }

    public static void main(String[] args) {
        new Tetris().start();
    }
}