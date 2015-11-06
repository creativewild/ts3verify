

// Copyright (C) 2015 by sourcemaker <https://github.com/sourcemaker>
// Dieses Programm ist freie Software. Sie können es unter den Bedingungen der GNU General Public License,
// wie von der Free Software Foundation veröffentlicht, weitergeben und/oder modifizieren, entweder gemäß
// Version 2 der Lizenz oder (nach Ihrer Option) jeder späteren Version.
//
// Die Veröffentlichung dieses Programms erfolgt in der Hoffnung, daß es Ihnen von Nutzen sein wird,
// aber OHNE IRGENDEINE GARANTIE, sogar ohne die implizite Garantie der MARKTREIFE oder der
// VERWENDBARKEIT FÜR EINEN BESTIMMTEN ZWECK. Details finden Sie in der GNU General Public License.
//
// Sie sollten ein Exemplar der GNU General Public License zusammen mit diesem Programm erhalten haben.
// Falls nicht, schreiben Sie an die Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA.
//
// Diese Software nutzt folgende Module:
// TeamSpeak 3 Java API <https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API> von TheHolyWaffle
// Google GSON <https://github.com/google/gson> von Google Inc.
// Mysql-connector <http://dev.mysql.com/> von Oracle

package de.gw2api.ts3verify;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.*;

public class gw2verify
{
    Level debug = Level.ALL;
    Config botconfig;

    protected HashMap ClientValues(ClientProperty property,
                                   String value) {
        HashMap hm = new HashMap();
        hm.put(property, value);
        return hm;
    }

    public static void main(String[] args) throws Exception
    {
        gw2verify jts3 = new gw2verify();
        jts3.runServerMod();
    }

    void runServerMod() throws Exception
    {
        botconfig = new Config();
        botconfig.load();

        final TS3Config config = new TS3Config();
        config.setHost(botconfig.prop.getProperty("ts3host"));
        config.setDebugLevel(debug);
        config.setFloodRate(TS3Query.FloodRate.DEFAULT);
        config.setLoginCredentials(botconfig.prop.getProperty("ts3user"), botconfig.prop.getProperty("ts3pass"));

        final TS3Query query = new TS3Query(config);
        try {
            query.connect();
        } catch (Exception ex) {
            System.out.println("Keine Verbindung zum Teamspeak-Server möglich. Programm wird beendet.");
            System.exit(0);
        }

        final TS3Api api = query.getApi();
        api.selectVirtualServerById(Integer.parseInt(botconfig.prop.getProperty("ts3inst")));
        api.setNickname(botconfig.prop.getProperty("ts3botname"));
        api.sendChannelMessage("TS3GW2verify is now online!");

        api.registerAllEvents();
        api.addTS3Listeners(new TS3Listener() {

            final int clientId = api.whoAmI().getId();

            public void onTextMessage(TextMessageEvent e) {
                if (e.getTargetMode() == TextMessageTargetMode.CLIENT && e.getInvokerId() != clientId) {
                    Pattern p = Pattern.compile("([\\w|\\d]{8}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{20}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{12})");
                    Matcher m = p.matcher(e.getMessage());
                    if (m.matches()) {
                        Gw2ApiReader gw2api;
                        try {
                            api.sendPrivateMessage(e.getInvokerId(), "API-Schlüssel wird geprüft. Bitte warten...");
                            gw2api = new Gw2ApiReader(e.getMessage());

                            String dbhost = "jdbc:mysql://"+botconfig.prop.getProperty("mysqlServer")+":"+botconfig.prop.getProperty("mysqlPort")+"/"+botconfig.prop.getProperty("mysqlDB");
                            Connection sqlconnect = DriverManager.getConnection(dbhost, botconfig.prop.getProperty("mysqlUser"), botconfig.prop.getProperty("mysqlPassword"));
                            if (sqlconnect.isClosed()) {
                                System.out.println("Der mySQL-Server ist nicht erreichbar. Programm wird beendet.");
                                api.sendPrivateMessage(e.getInvokerId(), "Der Datenbankserver ist nicht erreichbar. Der Bot ist daher nicht funktionsbereit.");
                                System.exit(1);
                            }
                            Statement checkStmt = sqlconnect.createStatement();
                            ResultSet res = checkStmt.executeQuery("SELECT COUNT(*) as total FROM `clients` WHERE `api_key`='"+gw2api.api_key+"' LIMIT 1");

                            if (res.next() && res.getInt("total") == 0) {
                                boolean validator = true;

                                // ***** check for server *****************************************
                                boolean checkServer = false;
                                int mustServer = 0;
                                if (botconfig.prop.getProperty("checkServer").equalsIgnoreCase("true")) {
                                    checkServer = true;
                                    mustServer = Integer.parseInt(botconfig.prop.getProperty("ServerID"));
                                }

                                // ***** check for Guild ******************************************
                                boolean checkGuild = false;
                                String mustGuild = "invalid";
                                if (botconfig.prop.getProperty("checkGuild").equalsIgnoreCase("true")) {
                                    checkGuild = true;
                                    mustGuild = botconfig.prop.getProperty("GuildID");
                                }
                                // ***** do the check **********************************************

                                if (checkServer) {
                                    validator = gw2api.isMemberOfServer(mustServer);
                                }

                                if (checkGuild) {
                                    if (!gw2api.isMemberOfGuild(mustGuild)) {
                                        validator = false;
                                    }
                                }

                                if (validator) {
                                    try {
                                        Statement myStmt = sqlconnect.createStatement();
                                        long unixTime = System.currentTimeMillis() / 1000L;
                                        myStmt.executeUpdate("INSERT INTO `clients` (`ts3_uniquekey`, `api_key`, `last_check`, `no_touch`) VALUES ('" + e.getInvokerUniqueId() + "', '" + gw2api.api_key + "', '" + unixTime + "', '0')");

                                        api.sendPrivateMessage(e.getInvokerId(), "Vielen Dank für deine Mithilfe unsere Daten aktuell zu halten. Du wurdest freigeschaltet.");
                                        api.sendPrivateMessage(e.getInvokerId(), "Beachte bitte, dass ein Löschen des API-Schlüssels auch die Beendigung der Mitgliedschaft hier hat!");
                                        ClientInfo clientInfo = api.getClientInfo(e.getInvokerId());
                                        api.addClientToServerGroup(Integer.parseInt(botconfig.prop.getProperty("ts3group_member")), clientInfo.getDatabaseId());
                                        if (botconfig.prop.getProperty("setDescription").equalsIgnoreCase("true")) {
                                            api.editClient(e.getInvokerId(), ClientValues(ClientProperty.CLIENT_DESCRIPTION, gw2api.getAccountName()));
                                        }

                                    } catch (Exception ex) {
                                        api.sendPrivateMessage(e.getInvokerId(), "Leider ist unsere Datenbank nicht erreichbar. Bitte kontaktiere einen Administrator.");
                                        ex.printStackTrace();
                                    }
                                } else {
                                    api.sendPrivateMessage(e.getInvokerId(), "Leider erfüllt der Account vom angegebenen API-Schlüssel nicht die aktuellen Anforderungen der automatischen Freischaltung.");
                                }
                            } else {
                                api.sendPrivateMessage(e.getInvokerId(), "Dieser API-Schlüssel ist bereits einem Teamspeak-Account zugeordnet. Bitte erstelle einen neuen/weiteren Schlüsel.");
                            }
                        } catch (UnknownHostException ex) {
                            api.sendPrivateMessage(e.getInvokerId(), "Der API-Server von Guild Wars 2 ist derzeit nicht erreichbar. Bitte versuche es später erneut.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            api.sendPrivateMessage(e.getInvokerId(), "Der eingegebebe API-Key ist ungültig. Bitte überprüfe deine Eingaben.");
                        }
                    }
                } else if (e.getTargetMode() == TextMessageTargetMode.CHANNEL && e.getInvokerId() != clientId) {
                    String message = e.getMessage().toLowerCase();

                    if (message.equalsIgnoreCase("!botinfo")) {
                        api.sendPrivateMessage(e.getInvokerId(), "GW2TS3verify Bot Version 1.0 ist aktiv");
                    } else if (message.equalsIgnoreCase("!botquit")) {
                        ClientInfo invoker = api.getClientInfo(e.getInvokerId());
                        for (int groupid: invoker.getServerGroups()) {
                            if (groupid == Integer.parseInt(botconfig.prop.getProperty("ts3group_admin"))) {
                                System.out.println("Bot wird heruntergefahren. Authorisation durch " + invoker.getNickname() + " ("+invoker.getUniqueIdentifier()+")");
                                api.sendPrivateMessage(invoker.getId(), "Bot wird heruntergefahren. Bye bye!");
                                System.exit(0);
                            }
                        }
                    } else if (message.equalsIgnoreCase("!botcheck")) {
                        ClientInfo invoker = api.getClientInfo(e.getInvokerId());
                        for (int groupid : invoker.getServerGroups()) {
                            if (groupid == Integer.parseInt(botconfig.prop.getProperty("ts3group_admin"))) {
                                api.sendPrivateMessage(e.getInvokerId(), "Vollständiger Abgleich der Datenbank mit der GuildWars 2 API");
                                api.sendPrivateMessage(e.getInvokerId(), "Dieser Vorgang kann u.U. einige Minuten in Anspruch nehmen. Sobald der Vorgang abgeschlossen ist, wird eine Meldung herausgegeben.");

                                String dbhost = "jdbc:mysql://"+botconfig.prop.getProperty("mysqlServer")+":"+botconfig.prop.getProperty("mysqlPort")+"/"+botconfig.prop.getProperty("mysqlDB");
                                Connection sqlconnect = null;
                                try {
                                    sqlconnect = DriverManager.getConnection(dbhost, botconfig.prop.getProperty("mysqlUser"), botconfig.prop.getProperty("mysqlPassword"));
                                    Statement checkStmt = sqlconnect.createStatement();
                                    Statement delStmt = sqlconnect.createStatement();
                                    ResultSet res = checkStmt.executeQuery("SELECT * FROM `clients` WHERE `no_touch`='0'");
                                    while (res.next()) {
                                        String uniqueID = res.getString("ts3_uniquekey");
                                        String api_key  = res.getString("api_key");
                                        try {
                                            Gw2ApiReader gw2api = new Gw2ApiReader(api_key);
                                            boolean validator = true;

                                            // ***** check for server *****************************************
                                            boolean checkServer = false;
                                            int mustServer = 0;
                                            if (botconfig.prop.getProperty("checkServer").equalsIgnoreCase("true")) {
                                                checkServer = true;
                                                mustServer = Integer.parseInt(botconfig.prop.getProperty("ServerID"));
                                            }

                                            // ***** check for Guild ******************************************
                                            boolean checkGuild = false;
                                            String mustGuild = "invalid";
                                            if (botconfig.prop.getProperty("checkGuild").equalsIgnoreCase("true")) {
                                                checkGuild = true;
                                                mustGuild = botconfig.prop.getProperty("GuildID");
                                            }
                                            // ***** do the check **********************************************

                                            if (checkServer) {
                                                validator = gw2api.isMemberOfServer(mustServer);
                                            }

                                            if (checkGuild) {
                                                if (!gw2api.isMemberOfGuild(mustGuild)) {
                                                    validator = false;
                                                }
                                            }

                                            if (!validator) {
                                                // we got someone! kick it
                                                int clientDBid = api.getClientByUId(uniqueID).getDatabaseId();
                                                api.removeClientFromServerGroup(Integer.parseInt(botconfig.prop.getProperty("ts3group_member")),clientDBid);
                                                delStmt.executeUpdate("DELETE FROM `clients` WHERE `api_key`='" + api_key + "'");
                                                api.sendPrivateMessage(e.getInvokerId(), "Mitgliedschaft entfernt: " + uniqueID);
                                            }
                                        } catch (Exception e2) {
                                            int clientDBid = api.getClientByUId(uniqueID).getDatabaseId();
                                            api.removeClientFromServerGroup(Integer.parseInt(botconfig.prop.getProperty("ts3group_member")),clientDBid);
                                            delStmt.executeUpdate("DELETE FROM `clients` WHERE `api_key`='" + api_key + "'");
                                            api.sendPrivateMessage(e.getInvokerId(), "Mitgliedschaft entfernt: " + uniqueID);
                                        }
                                    }
                                } catch (Exception e1) {
                                    api.sendPrivateMessage(e.getInvokerId(), "Bei der Datenbankabfrage ist ein Fehler aufgetreten. Der Vorgang wurde abgebrochen.");
                                    e1.printStackTrace();
                                }

                                api.sendPrivateMessage(e.getInvokerId(), "Überprüfung abgeschlossen.");
                            }
                        }
                    }
                }
            }

            public void onClientJoin(ClientJoinEvent e) {
                if (e.getClientServerGroups().equalsIgnoreCase(botconfig.prop.getProperty("ts3group_guest"))) {
                    api.pokeClient(e.getClientId(), "Willkommen auf unserem WvW TS! Um dich freizuschalten lese bitte den Chat unten");
                    api.sendPrivateMessage(e.getClientId(), "**************************************************************************************");
                    api.sendPrivateMessage(e.getClientId(), "Hallo " + e.getInvokerName() + "! Schön, dass du deinen Weg zu uns gefunden hast :)");
                    api.sendPrivateMessage(e.getClientId(), "Dieser Teamspeak unterstützt eine automatische Freischaltung. Gehe hierzu bitte auf [url]https://account.arena.net/applications[/url] und erstelle dir einen API-Schlüssel.");
                    api.sendPrivateMessage(e.getClientId(), "Wichtig ist, dass das Häkchen bei 'account' angeklickt ist.");
                    api.sendPrivateMessage(e.getClientId(), "Wenn du Hilfe brauchst oder Probleme hast, wende dich bitte an einen TS-Administrator.");
                    api.sendPrivateMessage(e.getClientId(), "**************************************************************************************");
                    api.sendPrivateMessage(e.getClientId(), "Wenn du fertig bist, gebe diesen Schlüssel hier ein:");
                }
            }

            // unused so far
            public void onServerEdit(ServerEditedEvent e) {}
            public void onClientMoved(ClientMovedEvent e) {}
            public void onClientLeave(ClientLeaveEvent e) {}
            public void onChannelEdit(ChannelEditedEvent e) {}
            public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {}
            public void onChannelCreate(ChannelCreateEvent e) {}
            public void onChannelDeleted(ChannelDeletedEvent e) {}
            public void onChannelMoved(ChannelMovedEvent e) {}
            public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {}
        });

        System.out.println("+*************************************************************");
        System.out.println("|   GW2Api.de - Teamspeak 3 Verifizierung für Guild Wars 2    ");
        System.out.println("+*************************************************************");
        System.out.println("|");
        System.out.println("| Der Bot wurde gestartet und ist auf dem Server aktiv.");
        System.out.println("| Statuscheck: !botinfo");
        System.out.println("| Bot beenden: !botquit");
        System.out.println("| Überprüfung sämtlicher Accounts: !botcheck (Kann sehr lange dauern)");
        System.out.println();

    }
}
