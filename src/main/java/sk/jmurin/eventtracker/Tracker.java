/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.jmurin.eventtracker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import javax.mail.*;
import javax.mail.internet.*;
import jdk.nashorn.internal.objects.NativeArray;
import org.jsoup.nodes.Element;

/**
 *
 * @author janmu
 */
public class Tracker {

    public enum EmailTypes {
        SUCCESS, REPORT, EXCEPTION;
    }

    public static void sendEmail(String recipient, String content, EmailTypes type) {
        System.out.println("sending email to [" + recipient + "] with content: [" + content + "] ");

        final String username = "";
        final String password = "";
        String subject = "";
        if (type == EmailTypes.SUCCESS) {
            subject = "SUCCESS: FREE SEATS";
        }
        if (type == EmailTypes.EXCEPTION) {
            subject = "EXCEPTION event tracker";
        }
        if (type == EmailTypes.REPORT) {
            subject = "REPORT: event tracker";
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.websupport.sk");
        props.put("mail.smtp.port", "25");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);

            System.out.println("Done");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Integer> findFreeSeats(String url, Set<String> ignored) throws Exception {
        JSoup jsoup = new JSoup();
        Map<String, Integer> ticketsCount = new HashMap<>();

        System.out.println("loading url: " + url);
        if (!ignored.isEmpty()) {
            System.out.println("ignoring seat ids: " + ignored);
        }

        Document page = jsoup.getPage(url);

        Elements reserved = page.select(".tickets-wrapper div[id*=\"place-\"].ticketSelect-section-place-reserved");
        ticketsCount.put("reserved", reserved.size());
        System.out.println("reserved tickets count: " + reserved.size());

        Elements sold = page.select(".tickets-wrapper div[id*=\"place-\"].ticketSelect-section-place-sold");
        ticketsCount.put("sold", sold.size());
        System.out.println("sold tickets count: " + sold.size());

        Elements free = page.select(".tickets-wrapper div[id*=\"place-\"].ticketSelect-section-place-free");
        int freeSize = free.size();
        Iterator<Element> iterator = free.iterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            String idAttr = e.attr("id");
            // decrement freeseats count on ignored seats
            if (ignored.contains(idAttr)) {
                freeSize--;
            }
        }
        ticketsCount.put("free", freeSize);
        System.out.println("free tickets count: " + freeSize);

        ticketsCount.put("ignored", free.size() - freeSize);
        System.out.println("ignored tickets count: " + ticketsCount.get("ignored"));

        Elements all = page.select(".tickets-wrapper div[id*=\"place-\"]");
        ticketsCount.put("all", all.size());
        System.out.println("all tickets count: " + all.size());

        return ticketsCount;
    }

    public static void main(String[] args) {
        String url = "";
        String recipient = "";
        Set<String> ignored = new HashSet<String>();
        boolean alwaysSend = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("--h") || args[i].equals("--help")) {
                System.out.println("USAGE: \n"
                        + "-h help\n"
                        + "-url urlString\n"
                        + "-recipient recipient@example.com\n"
                        + "-alwaysSend to send email everytime\n"
                        + "-ignore [1,abc,striing,345]");
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
            if (args[i].equals("-ignore")) {
                try {
                    String par = args[i + 1];
                    par = par.substring(1, par.lastIndexOf(']'));
                    String[] split = par.split(",");
                    for (int j = 0; j < split.length; j++) {
                        ignored.add(split[j]);
                    }
                } catch (Exception e) {
                    Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, e);
//                    StringWriter sw = new StringWriter();
//                    PrintWriter pw = new PrintWriter(sw);
//                    e.printStackTrace(pw);
//                    sendEmail(recipient, "ignore list parsing exception:\n" + sw.toString(), EmailTypes.EXCEPTION);
                }
            }
            if (args[i].equals("-alwaysSend")) {
                alwaysSend = true;
            }
        }

        if (url.equals("")) {
            System.out.println("no url specified. use -url urlString");
            return;
        }

        if (recipient.equals("")) {
            System.out.println("no recipient specified. use -recipient recipient@example.com");
            return;
        }

        System.out.println("\nCurrent time: " + new Date().toString());

        try {
            Map<String, Integer> seats = findFreeSeats(url, ignored);
            if (seats.isEmpty()) {
                sendEmail(recipient, "empty seats map. bad css selector??", EmailTypes.EXCEPTION);
            }

            int freeCount = seats.get("free");
            if (alwaysSend || freeCount > 0) {
                String content = "REPORT\n\n"
                        + "url: " + url + "\n"
                        + "event stats: " + seats.toString();
                sendEmail(recipient, content, freeCount > 0 ? EmailTypes.SUCCESS : EmailTypes.REPORT);
            } else {
                System.out.println("not sending email");
            }

        } catch (Exception ex) {
            Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);

            sendEmail(recipient, sw.toString(), EmailTypes.EXCEPTION);
        }
    }
}
