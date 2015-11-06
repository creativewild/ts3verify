

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Gw2ApiReader {

    String api_key = "";
    String account_name = "";
    String account_id = "";
    int world = 0;
    String guilds;

    private void init() throws Exception {
        String sURL = "https://api.guildwars2.com/v2/account?access_token=" + api_key;
        System.out.println(sURL);

        URL url = new URL(sURL);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.connect();

        JsonParser jp = new JsonParser(); //from gson
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
        JsonObject rootobj = root.getAsJsonObject();

        account_name = rootobj.get("name").getAsString();
        account_id = rootobj.get("id").getAsString();
        world = rootobj.get("world").getAsInt();
        guilds = rootobj.get("guilds").getAsJsonArray().toString();
    }

    public boolean isMemberOfGuild(String guildid) {
        if (guilds.contains(guildid)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isMemberOfServer(int worldid) {
        if (worldid == world) {
            return true;
        } else {
            return false;
        }
    }

    public Gw2ApiReader(String Apikey) throws Exception {
        api_key = Apikey;
        init();
    }

    public String getAccountName() {
        return account_name;
    }

}
