/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library.io;

import streamit.library.Filter;
import streamit.library.Channel;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.List;
import java.util.ArrayList;
import java.lang.*;

public class ImageDisplay extends Filter {

    int width;
    int height;
    JWindow displayWindow;
    BufferedImage firstImage;
    JTextField info;
    Timer timer;
   
    java.util.List<BufferedImage> imageList = new ArrayList<BufferedImage>();
    int currentImage = 0;

    public ImageDisplay(int width_in, int height_in) {
        width = width_in;
        height = height_in;

        firstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixel = new int[3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel[0] = 255;
                pixel[1] = 255;
                pixel[2] = 255;
                firstImage.getRaster().setPixel(x, y, pixel);
            }
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getScreenDevices()[0];
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle screen = gc.getBounds();

        displayWindow = new JWindow();
        displayWindow.setBounds((screen.width-width)/2+screen.x, 
                                (screen.height-height)/2+screen.y, 
                                width, 
                                height);
        BorderLayout borderlay = new BorderLayout();
        displayWindow.setContentPane(new JPanel(borderlay)
            {
                public void paint(Graphics g) {
                    if (imageList.size() == 0) {
                        g.drawImage(firstImage, 0, 0, this);
                        info.setText("Waiting for first image");
                    } else {
                        g.drawImage(imageList.get(currentImage - 1), 0, 0, this);
                        info.setText("Image " + currentImage + " of " + imageList.size());
                    }
                    paintChildren(g);
                }
            });

        displayWindow.getContentPane().setSize(width, height);

        info = new JTextField();
        info.setSelectionColor(Color.WHITE);
        info.setBorder(new EmptyBorder(0,0,0,0));
        info.setPreferredSize(new Dimension(130, 20));

        timer = new Timer(50, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    currentImage += 1;
                    if (currentImage > imageList.size())
                        currentImage = 1;
                    displayWindow.repaint();
                }
            });

        JButton buttonFrameBck = new JButton("<");
        buttonFrameBck.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                    currentImage -= 1;
                    if (currentImage <= 0) {
                        currentImage = 1;
                    }
                    displayWindow.repaint();
                }
            });
        JButton buttonFrameFwd = new JButton(">");
        buttonFrameFwd.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                    currentImage += 1;
                    if (currentImage > imageList.size()) {
                        currentImage = imageList.size();
                    }
                    displayWindow.repaint();
                }
            });

        JButton buttonPlay = new JButton("Play");
        buttonPlay.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timer.start();
                }
            });
        JButton buttonStop = new JButton("Stop");
        buttonStop.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                }
            });

        JWindow controlWindow = new JWindow();
        int controlWindow_width = 280;
        int controlWindow_height = 30;
        controlWindow.setBounds((screen.width-controlWindow_width)/2+screen.x, 
                                (screen.height-height)/2+screen.y+height+8, 
                                controlWindow_width, 
                                controlWindow_height);
        controlWindow.getContentPane().setBackground(Color.WHITE);

        Panel leftPanel = new Panel(new FlowLayout());
        Panel rightPanel = new Panel(new FlowLayout());

        controlWindow.getContentPane().add(leftPanel, BorderLayout.WEST);       
        controlWindow.getContentPane().add(rightPanel, BorderLayout.EAST);       

        Insets insets = new Insets(1, 1, 1, 1);
        buttonFrameBck.setMargin(insets);
        buttonFrameFwd.setMargin(insets);
        buttonPlay.setMargin(insets);
        buttonStop.setMargin(insets);

        leftPanel.add(info);
        rightPanel.add(buttonFrameBck);
        rightPanel.add(buttonFrameFwd);
        rightPanel.add(buttonPlay);
        rightPanel.add(buttonStop);

        /* -- Code Modified from Sun's Online Training Website, August 2nd 2002 --- */
        MouseInputListener mil = new WindowDragger(displayWindow);
        MouseInputListener mil2 = new WindowDragger(controlWindow);
        // Dragging the image window drags the control panel with it
        // Dragging the control panel moves only the control panel
        displayWindow.addMouseListener(mil);
        displayWindow.addMouseMotionListener(mil);
        displayWindow.addMouseListener(mil2);
        displayWindow.addMouseMotionListener(mil2);
        controlWindow.addMouseListener(mil2);
        controlWindow.addMouseMotionListener(mil2);
        /* --- 
         * http://java.sun.com/developer/onlineTraining/new2java/supplements/solutions/August02.html 
         * --- */

        displayWindow.show();
        controlWindow.show();

    }

    public void init() {
        inputChannel = new Channel (Integer.TYPE, width*height*3);
    }
 
    public void work() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixel = new int[3];    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel[0] = inputChannel.popInt();
                pixel[1] = inputChannel.popInt();
                pixel[2] = inputChannel.popInt();
                newImage.getRaster().setPixel(x, y, pixel);
            }
        }
        imageList.add(newImage);
        currentImage = imageList.size();
        displayWindow.repaint();
    }

    public void DELETE() {
        displayWindow.dispose();
    }

}

/* -- Code from Sun's Online Training Website, August 2nd 2002 --- */
class WindowDragger extends MouseInputAdapter {
    JWindow window;
    Point origin = new Point();

    public WindowDragger(JWindow window) {
        this.window = window;
    }

    public void mousePressed(MouseEvent e) {
        // Remember offset into window for dragging
        origin.x = e.getX();
        origin.y = e.getY();
    }

    public void mouseDragged(MouseEvent e) {
        // Move window relative to drag start
        Point p = window.getLocation();
        window.setLocation(
                           p.x + e.getX() - origin.x, 
                           p.y + e.getY() - origin.y);
    }

}
/* --- 
 * http://java.sun.com/developer/onlineTraining/new2java/supplements/solutions/August02.html 
 * --- */

