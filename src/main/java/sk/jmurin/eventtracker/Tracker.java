/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.jmurin.eventtracker;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import javax.mail.*;
import javax.mail.internet.*;

/**
 *
 * @author janmu
 */
public class Tracker {

    public static void sendEmail(String recipient, String content, boolean success) {
        System.out.println("sending email to [" + recipient + "] with content: [" + content + "] ");

        String host = "localhost";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(success ? "success-tracker@localhost.com" : "event-tracker@localhost.com"));

            // Set Subject: header field
            message.setSubject(success ? "SUCCESS: FREE SEATS" : "event tracker report");

            // Now set the actual message
            message.setText(content);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            Transport.send(message);

        } catch (MessagingException mex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, mex);
        }
    }

    public static Map<String, Integer> findFreeSeats(String url) {
        JSoup jsoup = new JSoup();
        Map<String, Integer> ticketsCount = new HashMap<>();

        try {
            System.out.println("loading url: " + url);
            Document page = jsoup.getPage(url);

            Elements reserved = page.select(".tickets-wrapper div[id*=\"place-\"].ticketSelect-section-place-reserved");
            ticketsCount.put("reserved", reserved.size());
            System.out.println("reserved tickets count: " + reserved.size());

            Elements sold = page.select(".tickets-wrapper div[id*=\"place-\"].ticketSelect-section-place-sold");
            ticketsCount.put("sold", sold.size());
            System.out.println("sold tickets count: " + sold.size());

            Elements free = page.select(".tickets-wrapper div[id*=\"place-\"].ticketSelect-section-place-free");
            ticketsCount.put("free", free.size());
            System.out.println("free tickets count: " + free.size());

            Elements all = page.select(".tickets-wrapper div[id*=\"place-\"]");
            ticketsCount.put("all", all.size());
            System.out.println("all tickets count: " + all.size());

            return ticketsCount;

        } catch (Exception ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
            // send email that link is unavailable

            return Collections.EMPTY_MAP;
        }
    }

    public static void main(String[] args) {
        String url = "";
        String recipient = "";
        boolean alwaysSend = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("--h") || args[i].equals("--help")) {
                System.out.println("USAGE: \n"
                        + "-h help\n"
                        + "-url urlString\n"
                        + "-recipient recipient@example.com\n"
                        + "-alwaysSend to send email everytime");
                return;
            }
            if (args[i].equals("-url")) {
                try {
                    url = args[i + 1];
                } catch (Exception e) {
                }
            }
            if (args[i].equals("-recipient")) {
                try {
                    recipient = args[i + 1];
                } catch (Exception e) {
                }
            }
            if (args[i].equals("-alwaysSend")) {
                alwaysSend = true;
            }
        }
        //url = "https://www.navstevnik.sk/vyber-vstupeniek?eventId=1028540";
        if (url.equals("")) {
            System.out.println("no url specified. use -url urlString");
            return;
        }
        //recipient = "jan.murin@globallogic.com";
        if (recipient.equals("")) {
            System.out.println("no recipient specified. use -recipient recipient@example.com");
            return;
        }

        System.out.println("\nCurrent time: " + new Date().toString());
        Map<String, Integer> seats = findFreeSeats(url);

        int freeCount = seats.get("free");
        if (alwaysSend || freeCount > 0) {
            String content = "REPORT\n\n"
                    + "url: " + url + "\n"
                    + "event stats: " + seats.toString();
            sendEmail(recipient, content, freeCount > 0);
        } else {
            System.out.println("not sending email");
        }
    }
}
