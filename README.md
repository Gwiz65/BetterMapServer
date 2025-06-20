# BetterMapServer
A Better Map for Wurm Unlimited - Server

- **Installation**
  - needs Ago's Server Modlauncher installed
  - needs Ago's ServerPack and HttpServer mods installed
  - extract bettermapserver-x.x.zip into Wurm Unlimited Server directory
 
 - **Map Name**
   - The map name is set in the table "SERVERS" in wurmlogin.db.

    `update "SERVERS" set "MAPNAME" = 'MyCoolMap' where local = 1`

   - Note: "MAPNAME" is the map name sent to the client, "NAME" is the name as seen in the "Server Name" field of the "Local Server" settings

- **BetterMapServer Features**
  - Creates a serverpack to be sent to client for use with [BetterMap client mod](https://github.com/Gwiz65/BetterMap/releases/latest)
  - Has three settings in config file that control what information is sent to the clients:

    | Setting | Default |
    | :--- | :--- |
    | allowDeeds | true |
    | allowStartTowns | true |
    | allowSoulfallStones| true |

- **Release Notes:**
  - Release 1.0 - Initial release.
  - Release 1.1 - Excluded disbanded deeds
