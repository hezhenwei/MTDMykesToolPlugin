package MykesTool;

import arc.*;
import arc.math.Mathf;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration.*;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MTDUtilPlugin extends Plugin{

    private String[] arrayDirtyWords = DirtyWords.GetDefaultWords();
    private List<String> ListDirtyWordsExt = new ArrayList<>();
    private final String m_strLogPrefix = "[MykesTool] ";
    private void reloadWords()
    {
        ListDirtyWordsExt.clear();
        String strFileName = "config/DirtyWords.txt";
        FileReader fr = null;
        try {
            fr = new FileReader(strFileName);
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        }
        if (fr != null) {
           //System.out.println("[MTDDirtyWordMask] Reading word mask configs.");
            Log.info(m_strLogPrefix+ "Reading word mask configs.");
            BufferedReader br=new BufferedReader(fr);
            String line="";
            int nCount = 0;
            do{
                try {
                    line=br.readLine();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                if( line != null) {
                    nCount++;
                    ListDirtyWordsExt.add(line);
                    //System.out.println(line);
                }
            } while (line!=null);
            //System.out.println("[MTDDirtyWordMask] Read " + Integer.toString(nCount) + " words.");
            Log.info(m_strLogPrefix+"Read " + nCount + " words.");
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                File f1 = new File(strFileName);
                f1.createNewFile();
                //System.out.println("[MTDDirtyWordMask] Create " +strFileName + " to store dirty phrases.");
                Log.info(m_strLogPrefix + "Create " +strFileName + " to store dirty phrases.");
            } catch (Exception e) {
                //System.out.println("[MTDDirtyWordMask] Create " +strFileName + " failed" + e.toString());
                Log.info(m_strLogPrefix + "Error! Create " +strFileName + " failed" + e);
            }
        }
    }

    //time task per sec
    public void TimeTask()
    {
        // check if enemy unit count
        if(Vars.state.rules.mode() != Gamemode.pvp)
        {
            int nTotalUnits = 0;
            for(Team it: Team.all )
            {
                if( it != Team.sharded) {
                    nTotalUnits += it.data().units.size;
                }
            }
            int nOurUnits = Team.sharded.data().units.size;
            int nTotalEnemy = nTotalUnits - nOurUnits;
            //Log.info("Total "+ nTotalUnits + " enemy "+nTotalEnemy);
            if( nTotalEnemy > 800)
            {
                int nToKill = nTotalEnemy - 780;
                Call.sendMessage("敌人人数超过800，可能造成卡顿，随机杀掉敌人单位");
                for(int i=0;i<nToKill;)
                {
                    for(Team it: Team.all )
                    {
                        //Log.info("Try team" + it);
                        if( it != Team.sharded && it.data().units.size > 0) {
                            Unit u = it.data().units.random();
                            if ( u != null) {
                                //Log.info("Killing "+ u);
                                u.kill();
                                ++i; // it may stuck the system when enemy count significantly drop. but it will not happen. i think.
                            }
                        }
                    }
                }
            }
        }
        // end of check enemy count

        Time.runTask(60f, ()-> TimeTask());
    }

    //called when game initializes
    @Override
    public void init(){
        Log.info("MykesH Mindustry MTD Plugin");
        Log.info("1.屏蔽脏字 Mask dirty words");
        Log.info("2.禁止拆卸物品源/液体源/电力源 not allow to break infinit sources");
        Log.info("3.自动燃烧额外资源 Allow burn extra resource discard map config");
        /*
        //listen for a block selection event
        Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildPlan() != null && event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer()){
                //player is the unit controller
                Player player = event.builder.getPlayer();

                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + player.name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });
        */

        // register time.
        Time.runTask(60f, ()-> TimeTask());

        // dirty words mask functions.
        this.reloadWords();

        // dirty words mask functions.
        Vars.netServer.admins.addChatFilter((player, text) -> {
            int size = arrayDirtyWords.length;
            for (String strToReplace : arrayDirtyWords) {
                int nPhraseLength = strToReplace.length();
                StringBuilder sb=new StringBuilder();
                for(int j=0;j<nPhraseLength;j++) {
                    sb.append("*");
                }
                text = text.replace(strToReplace, sb.toString());
            }
                for (String strToReplace : ListDirtyWordsExt) {
                    int nPhraseLength = strToReplace.length();
                    StringBuilder sb=new StringBuilder();
                    for(int j=0;j<nPhraseLength;j++) {
                        sb.append("*");
                    }
                    text = text.replace(strToReplace, sb.toString());
                }
            //player.sendMessage("try to do something for v008");
            return text;
        });

        //Vars.netServer.admins.addChatFilter((player, text) -> text.replace("111", "****"));

        Vars.netServer.admins.addActionFilter(action -> {
            // not allow to break any kind of source
            if( Vars.state.rules.mode() != Gamemode.sandbox &&
                    action.type == ActionType.breakBlock &&
                    ( action.block == Blocks.itemSource ||
                      action.block == Blocks.liquidSource ||
                      action.block == Blocks.powerSource) )
            {
                action.player.sendMessage(m_strLogPrefix + "禁止拆卸物品源/液体源/电力源 Breaking source not allowed.");
                return false;
            }
            return true;
        });

        Events.on(PlayEvent.class, event ->{
            // burn extra resource.
            Vars.state.rules.coreIncinerates = true;

            for(Tile t:Vars.world.tiles)
            {
                if(t.block().buildType == Blocks.itemSource)
                {
                    Log.info(("disable itemSource"));
                    t.block().breakable = false;
                }
                if(t.block().buildType == Blocks.powerSource)
                {
                    Log.info(("disable powerSource"));
                    t.block().breakable = false;
                }
                if(t.block().buildType == Blocks.liquidSource)
                {
                    Log.info(("disable liquidSource"));
                    t.block().breakable = false;
                }
            }
            // all kinds of source cannot be hit and destruct by enemy.
            Blocks.itemSource.destructible = false;
            Blocks.liquidSource.destructible = false;
            Blocks.powerSource.destructible = false;
            // all kinds of source cannot be replaced.
            Blocks.itemSource.replaceable = false;
            Blocks.liquidSource.replaceable = false;
            Blocks.powerSource.replaceable = false;
        });

    }


    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("reloadwordmask", "Reload word mask in config/DirtyWords.txt.", args -> this.reloadWords());
    }

    /*
    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){

        //register a simple reply command
        handler.<Player>register("reply", "<text...>", "A simple ping command that echoes a player's text.", (args, player) -> {
            player.sendMessage("You said: [accent] " + args[0]);
        });

        //register a whisper command which can be used to send other players messages
        handler.<Player>register("whisper", "<player> <text...>", "Whisper text to another player.", (args, player) -> {
            //find player by name
            Player other = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));

            //give error message with scarlet-colored text if player isn't found
            if(other == null){
                player.sendMessage("[scarlet]No player by that name found!");
                return;
            }

            //send the other player a message, using [lightgray] for gray text color and [] to reset color
            other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1]);
        });
    }

     */
}
