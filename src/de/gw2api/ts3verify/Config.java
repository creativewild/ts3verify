

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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Config {

    Properties prop = new Properties();

    public void load() {
        if (!new File("gw2ts3verify.config").isFile()) {
            System.out.println("Die Konfigurationsdatei konnte nicht gefunden werden.");
            System.out.println("Das Programm wird beendet.");
            System.exit(2);
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream("gw2ts3verify.config");
            prop.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
