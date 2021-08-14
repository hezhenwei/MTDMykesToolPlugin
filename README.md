## <font color="#775555">For User</font>
### What it does
Used in Mindustry Server side.<br />

MykesH Mindustry MTD Plugin <br />
1.屏蔽脏字 Mask dirty words<br />
2.禁止拆卸物品源/液体源/电力源 not allow to break infinit sources<br />
3.自动燃烧额外资源 Allow burn extra resource discard map config<br />
### Installing

Simply place the .jar file
in your server's `config/mods` directory.<br />
Then restart the server. Now it works.<br />
To list your currently installed plugin/mods, run `mods` command at server side.

### Config
After it run for the first time, file "config/DirtyWords.txt" will be created.<br />
You can add words you want to mask.<br />
One line per phrase.<br />
Ater you change "config/DirtyWords.txt", type `reloadwordmask` at server to reload.<br />
No need to restart server<br />

#
#
## For Developer
### Building a Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.


## todo
I don't know

