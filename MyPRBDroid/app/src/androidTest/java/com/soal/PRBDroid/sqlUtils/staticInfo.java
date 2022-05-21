package com.soal.PRBDroid.sqlUtils;


import com.soal.PRBDroid.LogUtil;
import com.soal.PRBDroid.model.edge;
import com.soal.PRBDroid.model.element;
import com.soal.PRBDroid.model.helpReadStaticInfo;
import com.soal.PRBDroid.model.window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

//读取静态分析的结果：先模仿把所有的静态信息获取全名再说吧（待测活动名，跳转信息）
//1.先全部读入，输出读入的信息判断结果
//2.读取后的信息处理： 哪些数据有用，用在哪里？
/*
* edge：也加不到
*
*
* */
public class staticInfo {
    public static staticInfo instance=null;
    // original info
    public List<EDGE> eedges=new ArrayList<>();
    public List<WIDGET> wwidgets=new ArrayList<>();
    public List<WINDOW> wwindows=new ArrayList<>();
    // handled info  （这是个逐步迭代的过程，不一定是最终版本）
    public List<window> ActWindows=new ArrayList<>();// 集合了所有的相关结点的 widget，确定这个里面的所有权限，在加一个标识符 isActTest= 0不测，1，活动测，2.只测控件
    public Map<String,window> ActNametoWindows=new HashMap<>();  //name to Window
    public List<edge>  staticEdges=new ArrayList<>();  //先不处理（只考虑）

    //考虑添加一个 危险权限集 ，也是从SQL中读取 （暂时替代品是，从其他表中的permissions属性读取,而不是该APP申请的危险权限）
    public Set<String> PPermissions=new HashSet<>();
    public Set<String> staticPermissions=new HashSet<>();
    //有可能PPermossions为空，跟APP的危险权限不一样。这就可能需要APP的危险权限了。
    public staticInfo() {
    }
    public static staticInfo V(){
        if (instance==null){
            synchronized (staticInfo.class){
                if (instance==null){
                    instance=new staticInfo();
                }
            }
        }
        return instance;
    }
    // 数字默认值为 0； 字符串默认值为 null
    public static class EDGE{
        public long ID;
        public String edgeLabel;   //Open OptionsMenu
        public long srcID;
        public long tgtID;
        public long widgetID;    //  0 或者其他
    }
    public static class WIDGET{
        public long ID;
        public long winID;
        public String winName ;   //org.asdtm.goodweather.MainActivity      OPTIONSMENU8
        public String type;       //android.view.MenuItem
        public int resID;       //  0 或者其他  2131689673
        public String resName;   // null  或者其他  fab
        public String text;
        public String eventType;  //gen  存的时候似乎跟type一样，但是真实内容应该是CLICK, LONGCLICK,SCROLL,DRAG,HOVER,TOUCH,LEFTSLIDE,RIGHTSLIDE;
        public int subMenuID;
        public String  itemIDString;
        public int  itemID;
        public boolean isWidgetTest;
        public String permissions;
    }
    public static class WINDOW{
        public long ID;
        public String winName;
        public String type;
        public long optionsMenuID;
        public long contextMenuID;
        public long leftDrawerID;
        public long rightDrawerID;
        public int fragSizes;
        public String fragIDString;
        public int widgetSizes;
        public boolean isTest;
        public String permissions;
        public int isActivityTest;
    }
    public void getAllStaticInfo(String pkgName){

        //read dangerousPermissions.txt
        helpReadStaticInfo helper=new helpReadStaticInfo(pkgName);
        String pers=helper.getStaticDanPermissions();
        if(pers==null){
            LogUtil.V().logSql(pkgName+" con't find file.");
        }else{
            LogUtil.V().logSql("contents:"+pers);
            String [] permissions1= pers.split(" ");
            for (int i=0;i<permissions1.length;i++){
                LogUtil.V().logSql(i+"：android.permission."+permissions1[i]);
                staticPermissions.add("android.permission."+permissions1[i]);
            }
            LogUtil.V().logSql("Size："+staticPermissions.size());
        }

        //Call the database read in class to read in the information respectively
        WidgetDao.V().getWidgets();
        EdgeDao.V().getEdges();
        WindowNodeDao.V().getWindows();
        printStaticInfo();
    }
    public void printStaticInfo(){
        //Print out the information ：1.edge,2.widget,3.window
        Iterator<EDGE> iterator1=eedges.iterator();
        LogUtil.V().logSql("——————Edges——————");
        while (iterator1.hasNext()){
            EDGE tempEDGE=iterator1.next();
            String line="<ID:"+tempEDGE.ID+",label:"+tempEDGE.edgeLabel+",srcID:"+tempEDGE.srcID+",tgtID"+tempEDGE.tgtID+",widID:"+tempEDGE.widgetID+">";
            LogUtil.V().logSql(line);
        }
        LogUtil.V().logSql("——————Edges——————");
        Iterator<WIDGET> iterator2=wwidgets.iterator();
        LogUtil.V().logSql("——————Widgets——————");
        while (iterator2.hasNext()){
            WIDGET temp=iterator2.next();
            String line="<ID:"+temp.ID+",winID:"+temp.winID+",winName:"+temp.winName+",type"+temp.type+",resID:"+temp.resID;
            line+=",resName:"+temp.resName+",text:"+temp.text+",subMenuID:"+temp.subMenuID+",itemIDString"+temp.itemIDString+",itemID:"+temp.itemID+",isWidgetTest:"+temp.isWidgetTest+",permissions:"+temp.permissions+">";
            LogUtil.V().logSql(line);
        }
        LogUtil.V().logSql("——————Widgets——————");
        Iterator<WINDOW> iterator3=wwindows.iterator();
        LogUtil.V().logSql("——————States——————");
        while (iterator3.hasNext()){
            WINDOW temp=iterator3.next();
            String line="<ID:"+temp.ID+",winName:"+temp.winName+",type"+temp.type+",optionsMenuID:"+temp.optionsMenuID+",contextMenuID:"+temp.contextMenuID+",leftDrawerID:"+temp.leftDrawerID+",rightDrawerID:"+temp.rightDrawerID;
            line+=",fragSizes:"+temp.fragSizes+",fragIDString:"+temp.fragIDString+",widgetSizes:"+temp.widgetSizes+",isTest:"+temp.isTest+",permissions:"+temp.permissions+",isActivityTest:"+temp.isActivityTest+">";
            LogUtil.V().logSql(line);
        }
        LogUtil.V().logSql("——————States——————");

    }
    public void handleStaticInfo(){
        /*
        * 状态结点：winID, name, onClickElements, EleIDs，clickedEleIDs, finished ,Xpath  （外加   permissions  isActivityTest ）
        *元素：
        * 1.静态信息处理（重新构建状态结点和边）：
（1）.（主要）
以act为根本，将（碎片s+Menus+Drawers),全部收集到act中。
有多少act就有多少主状态：id（集合） name, isTest,isActTest,permission,widgets
（2）.跳转（次要）
获得新的跳转（）
* "android.permission.WRITE_EXTERNAL_STORAGE" "android.permission.WRITE_EXTERNAL_STORAGE"  "android.permission.WRITE_EXTERNAL_STORAGE"  "android.permission.WRITE_EXTERNAL_STORAGE"  "android.permission.WRITE_EXTERNAL_STORAGE"
        * */

        Iterator<WINDOW> iterator=wwindows.iterator();
        while (iterator.hasNext()){
            WINDOW oneActivity =iterator.next();
            if (oneActivity.type.equals("ACTIVITY")){
                window tempWin=new window();
                String s=oneActivity.winName;
                String name=s.substring(s.lastIndexOf(".")+1);
                tempWin.setName(name);  // org.asdtm.goodweather.MainActivity
                tempWin.setIsActivityTest(oneActivity.isActivityTest);  //  isActTest= 0不测，1，活动测，2.只测控件
                tempWin.setPermissions(oneActivity.permissions);  //android.permission.ACCESS_FINE_LOCATION android.permission.ACCESS_FINE_LOCATION
                //
                if (oneActivity.fragSizes>0){
                    String[] frags=oneActivity.fragIDString.split(" ");
                    for (int i=0;i<frags.length;i++){
                        long id= Long.valueOf(frags[i]);
                        LogUtil.V().logSql("frag id:"+id);

                        //找到对应，WINDOW,然后，将他的widget,permission加入avt中
                        List<WINDOW> windows1=wwindows;    //我也不知道这样有没有影响
                        Iterator<WINDOW> iterator2=windows1.iterator();
                        while (iterator2.hasNext()){
                            WINDOW tempWin1=iterator2.next();
                            if (tempWin1.ID==id){  //找到了属于自己的碎片 id
                                if(tempWin1.widgetSizes>0){  //将frg的控件加入自己的控件集合
                                    Iterator<WIDGET> iterator21=wwidgets.iterator();
                                    while (iterator21.hasNext()){
                                        WIDGET oneWidget=iterator21.next();
                                        if (oneWidget.winID==tempWin1.ID){  //找到了该frg的控件
                                            element tempEle=new element();
                                            tempEle.setEleClass(oneWidget.type);  // android.view.MenuItem
                                            if (oneWidget.resName!=null){
                                                tempEle.setResName(oneWidget.resName);// null   or  fab
                                            }
                                            if (oneWidget.text!=null){
                                                tempEle.setText(oneWidget.text);   //Update;
                                            }
                                            //if(itemIDString !=null ){ 该控件是 subMenu, 要处理该控件内的 widget}     //暂时先省略，
                                            if(oneWidget.permissions!=null){
                                                //控件的permission也加入到活动里面（可能在静态里面已经计算过了）
                                                if (!tempWin.getPermissions().contains(oneWidget.permissions)){
                                                    tempWin.addPermissions(oneWidget.permissions);    //permission
                                                }
                                            }
                                            //ele jion to
//                                            LogUtil.V().logSql("frg:"+id+",Type class ："+tempEle.getEleClass());
                                            tempWin.getOnClickElements().add(tempEle);
                                        }

                                    }
                                }
                                if(tempWin1.isActivityTest==1){
                                    tempWin.setIsActivityTest(1);
                                }else if(tempWin1.isActivityTest==2 && tempWin.getIsActivityTest()==0){
                                    tempWin.setIsActivityTest(2);
                                }
                                break;//跳出windows，因为只有一个对应Menu
                            }
                        }


                    }//for
                }

                if (oneActivity.widgetSizes>0){
                    //将widget中对应的结点找到，然后生成 element ,最后加入到
                    //tempWin.getOnClickElements().add(element e);
                    Iterator<WIDGET> iterator1=wwidgets.iterator();
                    while (iterator1.hasNext()){
                        WIDGET oneWidget=iterator1.next();
                        if (oneWidget.winID==oneActivity.ID){  //找到了自己的控件
                            element tempEle=new element();
                            tempEle.setEleClass(oneWidget.type);  // android.view.MenuItem
                            if (oneWidget.resName!=null){           // null   or  fab
                                tempEle.setResName(oneWidget.resName);
                            }
                            if (oneWidget.text!=null){
                                tempEle.setText(oneWidget.text);   //Update;
                            }
                            //if(itemIDString !=null ){ 该控件是 subMenu, 要处理该控件内的 widget}     //暂时先省略，
                            if(oneWidget.permissions!=null){
                                //控件的permission也加入到活动里面（可能在静态里面已经计算过了）
                                if (!tempWin.getPermissions().contains(oneWidget.permissions)){
                                    tempWin.addPermissions(oneWidget.permissions);
                                }
                            }
                            //ele加入活动结点中
                            tempWin.getOnClickElements().add(tempEle);
                        }
                    }

                }
                if (oneActivity.optionsMenuID!=0){
                    //找到对应，WINDOW,然后，将他的widget,permission加入avt中
                    List<WINDOW> windows1=wwindows;    //我也不知道这样有没有影响
                    Iterator<WINDOW> iterator2=windows1.iterator();
                    while (iterator2.hasNext()){
                        WINDOW tempWin1=iterator2.next();
                        if (tempWin1.ID==oneActivity.optionsMenuID){  //找到了自己的Menus
                            if(tempWin1.widgetSizes>0){  //将Menu的控件加入自己的控件集合
                                Iterator<WIDGET> iterator21=wwidgets.iterator();
                                while (iterator21.hasNext()){
                                    WIDGET oneWidget=iterator21.next();
                                    if (oneWidget.winID==tempWin1.ID){  //找到了MenuWindow自己的控件
                                        element tempEle=new element();
                                        tempEle.setEleClass(oneWidget.type);  // android.view.MenuItem
                                        if (oneWidget.resName!=null){
                                            tempEle.setResName(oneWidget.resName);// null   or  fab
                                        }
                                        if (oneWidget.text!=null){
                                            tempEle.setText(oneWidget.text);   //Update;
                                        }
                                        //if(itemIDString !=null ){ 该控件是 subMenu, 要处理该控件内的 widget}     //暂时先省略，
                                        if(oneWidget.permissions!=null){
                                            //控件的permission也加入到活动里面（可能在静态里面已经计算过了）
                                            if (!tempWin.getPermissions().contains(oneWidget.permissions)){
                                                tempWin.addPermissions(oneWidget.permissions);    //permission
                                            }
                                        }
                                        //ele加入活动结点中
                                        tempWin.getOnClickElements().add(tempEle);
                                    }

                                }
                            }
                            if(tempWin1.isActivityTest==1){
                                tempWin.setIsActivityTest(1);
                            }else if(tempWin1.isActivityTest==2 && tempWin.getIsActivityTest()==0){
                                tempWin.setIsActivityTest(2);
                            }
                            break;//跳出windows，因为只有一个对应Menu
                        }
                    }

                }
                if (oneActivity.contextMenuID!=0){
                    //找到对应，WINDOW,然后，将他的widget,permission加入avt中
                    List<WINDOW> windows1=wwindows;    //我也不知道这样有没有影响
                    Iterator<WINDOW> iterator2=windows1.iterator();
                    while (iterator2.hasNext()){
                        WINDOW tempWin1=iterator2.next();
                        if (tempWin1.ID==oneActivity.contextMenuID){  //找到了自己的Menus
                            if(tempWin1.widgetSizes>0){  //将Menu的控件加入自己的控件集合
                                Iterator<WIDGET> iterator21=wwidgets.iterator();
                                while (iterator21.hasNext()){
                                    WIDGET oneWidget=iterator21.next();
                                    if (oneWidget.winID==tempWin1.ID){  //找到了MenuWindow自己的控件
                                        element tempEle=new element();
                                        tempEle.setEleClass(oneWidget.type);  // android.view.MenuItem
                                        if (oneWidget.resName!=null){
                                            tempEle.setResName(oneWidget.resName);// null   or  fab
                                        }
                                        if (oneWidget.text!=null){
                                            tempEle.setText(oneWidget.text);   //Update;
                                        }
                                        //if(itemIDString !=null ){ 该控件是 subMenu, 要处理该控件内的 widget}     //暂时先省略，
                                        if(oneWidget.permissions!=null){
                                            //控件的permission也加入到活动里面（可能在静态里面已经计算过了）
                                            if (!tempWin.getPermissions().contains(oneWidget.permissions)){
                                                tempWin.addPermissions(oneWidget.permissions);    //permission
                                            }
                                        }
                                        //ele加入活动结点中
                                        tempWin.getOnClickElements().add(tempEle);
                                    }

                                }
                            }
                            if(tempWin1.isActivityTest==1){
                                tempWin.setIsActivityTest(1);
                            }else if(tempWin1.isActivityTest==2 && tempWin.getIsActivityTest()==0){
                                tempWin.setIsActivityTest(2);
                            }
                            break;//跳出windows
                        }
                    }

                }
                if (oneActivity.leftDrawerID!=0){
                    //找到对应，WINDOW,然后，将他的widget,permission加入avt中
                    List<WINDOW> windows1=wwindows;    //我也不知道这样有没有影响
                    Iterator<WINDOW> iterator2=windows1.iterator();
                    while (iterator2.hasNext()){
                        WINDOW tempWin1=iterator2.next();
                        if (tempWin1.ID==oneActivity.leftDrawerID){  //找到了自己的Menus
                            if(tempWin1.widgetSizes>0){  //将Menu的控件加入自己的控件集合
                                Iterator<WIDGET> iterator21=wwidgets.iterator();
                                while (iterator21.hasNext()){
                                    WIDGET oneWidget=iterator21.next();
                                    if (oneWidget.winID==tempWin1.ID){  //找到了MenuWindow自己的控件
                                        element tempEle=new element();
                                        tempEle.setEleClass(oneWidget.type);  // android.view.MenuItem
                                        if (oneWidget.resName!=null){
                                            tempEle.setResName(oneWidget.resName);// null   or  fab
                                        }
                                        if (oneWidget.text!=null){
                                            tempEle.setText(oneWidget.text);   //Update;
                                        }
                                        //if(itemIDString !=null ){ 该控件是 subMenu, 要处理该控件内的 widget}     //暂时先省略，
                                        if(oneWidget.permissions!=null){
                                            //控件的permission也加入到活动里面（可能在静态里面已经计算过了）
                                            if (!tempWin.getPermissions().contains(oneWidget.permissions)){
                                                tempWin.addPermissions(oneWidget.permissions);    //permission
                                            }
                                        }
                                        //ele加入活动结点中
                                        tempWin.getOnClickElements().add(tempEle);
                                    }

                                }
                            }
                            if(tempWin1.isActivityTest==1){
                                tempWin.setIsActivityTest(1);
                            }else if(tempWin1.isActivityTest==2 && tempWin.getIsActivityTest()==0){
                                tempWin.setIsActivityTest(2);
                            }
                            break;//跳出windows
                        }
                    }
                }
                if (oneActivity.rightDrawerID!=0){
                    //找到对应，WINDOW,然后，将他的widget,permission加入avt中
                    List<WINDOW> windows1=wwindows;    //我也不知道这样有没有影响
                    Iterator<WINDOW> iterator2=windows1.iterator();
                    while (iterator2.hasNext()){
                        WINDOW tempWin1=iterator2.next();
                        if (tempWin1.ID==oneActivity.rightDrawerID){  //找到了自己的Menus
                            if(tempWin1.widgetSizes>0){  //将Menu的控件加入自己的控件集合
                                Iterator<WIDGET> iterator21=wwidgets.iterator();
                                while (iterator21.hasNext()){
                                    WIDGET oneWidget=iterator21.next();
                                    if (oneWidget.winID==tempWin1.ID){  //找到了MenuWindow自己的控件
                                        element tempEle=new element();
                                        tempEle.setEleClass(oneWidget.type);  // android.view.MenuItem
                                        if (oneWidget.resName!=null){
                                            tempEle.setResName(oneWidget.resName);// null   or  fab
                                        }
                                        if (oneWidget.text!=null){
                                            tempEle.setText(oneWidget.text);   //Update;
                                        }
                                        //if(itemIDString !=null ){ 该控件是 subMenu, 要处理该控件内的 widget}     //暂时先省略，
                                        if(oneWidget.permissions!=null){
                                            //控件的permission也加入到活动里面（可能在静态里面已经计算过了）
                                            if (!tempWin.getPermissions().contains(oneWidget.permissions)){
                                                tempWin.addPermissions(oneWidget.permissions);    //permission
                                            }
                                        }
                                        //ele加入活动结点中
                                        tempWin.getOnClickElements().add(tempEle);
                                    }

                                }
                            }
                            if(tempWin1.isActivityTest==1){
                                tempWin.setIsActivityTest(1);
                            }else if(tempWin1.isActivityTest==2 && tempWin.getIsActivityTest()==0){
                                tempWin.setIsActivityTest(2);
                            }
                            break;//跳出windows
                        }
                    }
                }
                LogUtil.V().logSql("Act Name："+tempWin.getName());
                ActNametoWindows.put(tempWin.getName(),tempWin);

            } //只处理 activity
        } //window窗口迭代结束

    }
    public void printFinalStaticInfo(){
//        public List<window> ActWindows=new ArrayList<>();// 集合了所有的相关结点的 widget，确定这个里面的所有权限，在加一个标识符 isActTest= 0不测，1，活动测，2.只测控件
//        public List<edge>  staticEdges=new ArrayList<>();  //先不处理（只考虑）
        //将上面的结果输出打印一下，验证正确性
        //先不考虑edge了，因为没什么大用
        StringBuilder per=new StringBuilder();
        Iterator<window> iterator=ActNametoWindows.values().iterator();
        while (iterator.hasNext()){
            window act=iterator.next();
            StringBuilder stringBuffer=new StringBuilder();  //速度略快，但是不是线程安全
            stringBuffer.append("ClassName:"+act.getName()).append(", isActTest:"+act.getIsActivityTest());
            if (act.getPermissions()!=null){
                per.append(" "+act.getPermissions());
                stringBuffer.append(",Permissions:"+act.getPermissions());
            }
            stringBuffer.append(",Widgets {");
            Iterator<element> iterator1=act.getOnClickElements().iterator();
            StringBuilder stringBuilder=new StringBuilder();
            while (iterator1.hasNext()){
                element ele=iterator1.next();

                stringBuilder.append("[type："+ele.getEleClass());
                if (ele.getResName()!=null){
                    stringBuilder.append(",ResName："+ele.getResName());
                }
                if(ele.getText()!=null){
                    stringBuilder.append(",text："+ele.getText());
                }
                stringBuilder.append(" ] ");
            }
            stringBuffer.append(stringBuilder.toString());
            stringBuffer.append(" }");
            LogUtil.V().logSql(stringBuffer.toString());
        }
        LogUtil.V().logSql("_________dangerous permissions__________");
        String [] permission1= per.toString().split(" ");
        for (int i=0;i<permission1.length;i++){
            if(permission1[i].contains("android"))
            {PPermissions.add(permission1[i]);}
        }
        LogUtil.V().logSql("STG find permissions's size："+PPermissions.size());  //打印危险权限s
        Iterator<String> p1=PPermissions.iterator();
        while (p1.hasNext()){
            String a1=p1.next();
            LogUtil.V().logSql(a1);
        }
        LogUtil.V().logSql("STATIC find all permissions's size："+staticPermissions.size());  //打印危险权限s
        Iterator<String> p2=staticPermissions.iterator();
        while (p2.hasNext()){
            String a2=p2.next();
            LogUtil.V().logSql(a2);
        }

    }

    /*
    * ClassName:MainActivity, isActTest:1,Permissions:android.permission.ACCESS_FINE_LOCATION  android.permission.ACCESS_COARSE_LOCATION  ,
    * 控件集 {[类型：android.support.design.widget.FloatingActionButton,ResName：fab ]
    *  [类型：android.view.MenuItem,text：Search City ]
    * [类型：android.view.MenuItem,text：Detect Location ]
    *  [类型：android.view.MenuItem,text：Update ]  }
05-27 16:01:54.126  1828  1842 D sql     : ClassName:SearchActivity, isActTest:0,Permissions:,控件集 { }
05-27 16:01:54.126  1828  1842 D sql     : ClassName:LicenseActivity, isActTest:0,Permissions:,控件集 { }
05-27 16:01:54.126  1828  1842 D sql     : ClassName:WeatherForecastActivity, isActTest:0,Permissions:,控件集 {[类型：android.view.MenuItem,text：Update ]  }
05-27 16:01:54.126  1828  1842 D sql     : ClassName:SettingsActivity, isActTest:0,Permissions:,控件集 { }
05-27 16:01:54.126  1828  1842 D sql     : ClassName:GraphsActivity, isActTest:0,Permissions:,控件集 {[类型：android.view.MenuItem,text：Toggle Y-Axis ] [类型：android.view.MenuItem,text：Update ] [类型：android.view.MenuItem,text：Toggle Values ]  }
05-27 16:01:54.126  1828  1842 D sql     : __________涉及的危险权限_________
05-27 16:01:54.126  1828  1842 D sql     :  android.permission.ACCESS_FINE_LOCATION  android.permission.ACCESS_COARSE_LOCATION
05-27 16:01:54.126  1828  1842 D sql     : 数量：2
*
* 主活动————————————
* 待测状态信息：window{winID=-1782313293, name='MainActivity', onClickElements.size=5, EleIDs.size=5, clickedEleIDs.size=0, finished=false,
* Xpaths='class:android.widget.ImageButton,contant_desc:Open navigation drawer,text:null,resName:null,isClickable:true
* ||class:android.widget.TextView,contant_desc:Update,text:null,resName:org.asdtm.goodweather:id/main_menu_refresh,isClickable:true
* ||class:android.widget.TextView,contant_desc:Search City,text:null,resName:org.asdtm.goodweather:id/main_menu_search_city,isClickable:true
* ||class:android.widget.TextView,contant_desc:Detect Location,text:null,resName:org.asdtm.goodweather:id/main_menu_detect_location,isClickable:true
* ||class:android.widget.ImageButton,contant_desc:null,text:null,resName:org.asdtm.goodweather:id/fab,isClickable:true'}
* 侧边栏————————————
* 待测状态信息：window{winID=1414949647, name='MainActivity', onClickElements.size=6, EleIDs.size=6, clickedEleIDs.size=0, finished=false,
* Xpaths='class:android.widget.CheckedTextView,contant_desc:null,text:Current Weather,resName:org.asdtm.goodweather:id/design_menu_item_text,isClickable:true
* ||class:android.widget.CheckedTextView,contant_desc:null,text:Graphs,resName:org.asdtm.goodweather:id/design_menu_item_text,isClickable:true
* ||class:android.widget.CheckedTextView,contant_desc:null,text:Daily Forecast,resName:org.asdtm.goodweather:id/design_menu_item_text,isClickable:true
* ||class:android.widget.CheckedTextView,contant_desc:null,text:Settings,resName:org.asdtm.goodweather:id/design_menu_item_text,isClickable:true
* ||class:android.widget.CheckedTextView,contant_desc:null,text:Feedback,resName:org.asdtm.goodweather:id/design_menu_item_text,isClickable:true
* ||class:android.widget.CheckedTextView,contant_desc:null,text:Donate Bitcoin,resName:org.asdtm.goodweather:id/design_menu_item_text,isClickable:true'}
*
    * */
    public int getTestActState(window  testAct){  //(传入参数要变为 window testAct) 初版，判断仅仅依靠 activity name （后果，导致有一些可能需要多次重复探索，浪费时间。需要根据（∩∪关系计算） 交并比（Intersection-over-Union，IoU），目标检测中使用的一个概念，是产生的候选框（candidate bound）与原标记框（ground truth bound）的交叠率，即它们的交集与并集的比值。最理想情况是完全重叠，即比值为1。）
        //Forecasite的当前活动名是这样的， 这就奇怪了，需要判断
        //[状态ID：-1365511923] [活动名：activities.MainActivity] [OnclickEle：8] [eleID：8]
        //6.29 changes
        String a=testAct.getName();
        String b=testAct.getName();
        LogUtil.V().log("最初活动名"+b);
        if(a.contains(".")) {
            int index=a.lastIndexOf(".");
            b=a.substring(index+1);
            LogUtil.V().log("去点之后的活动名"+b);
        }
        //

        window awin=ActNametoWindows.get(b);
        if (awin !=null){
            LogUtil.V().log("——staticInfo——Current activity name："+testAct.getName()+",在图中");
            //非 0 即1、2才判断，   如果当前找到了至少一个对应静态状态的控件，则赋值1、2，否则表示当前状态很可能不包含待测目标，返回0
            if(awin.getIsActivityTest()!=0){
                //静态图中的每个可点击控件 widget
                int count=0;
                Iterator<element> stgEles=awin.getOnClickElements().iterator();
                while (stgEles.hasNext()){
                    element aStgEle=stgEles.next();
                    Iterator<element> testEles=testAct.getOnClickElements().iterator();
                    while (testEles.hasNext()){
                        element aTestEle=testEles.next();
                        if(aStgEle.getText()!=null && aTestEle.getContent_desc()!=null){
                            if(aStgEle.getText().equals(aTestEle.getContent_desc())){
                                count++;
                                break;  //jomp to next StgElement
                            }
                        }
                        if(aStgEle.getResName()!=null && aTestEle.getResName()!=null){
                            if(aTestEle.getResName().contains(aStgEle.getResName())){
                                count++;
                                break;
                            }
                        }
                    }

                }
                if(count>0){
                    return awin.getIsActivityTest();
                }else{
                    return 1;//本来应该是0 的，但是为了覆盖率变成了1 当前状态并不是目标状态
                }
            }

            return awin.getIsActivityTest();
        }else{
            LogUtil.V().log("——staticInfo—— the name of the current state is："+testAct.getName()+",不在图中,");
            return 0;
        }
    }
    // 有些有双引号有些没有 android.permission.GET_ACCOUNTS   "android.permission.WRITE_EXTERNAL_STORAGE" "android.permission.WRITE_EXTERNAL_STORAGE"  "android.permission.WRITE_EXTERNAL_STORAGE"  "android.permission.WRITE_EXTERNAL_STORAGE"  "android.permission.WRITE_EXTERNAL_STORAGE"
    public String[] getTestActPermissions(String  testActName){  // 初版，判断仅仅依靠 activity name （后果，导致有一些可能需要多次重复探索，浪费时间。 ）
        //6.29 changes
        String a=testActName;
        String b=testActName;
        if(a.contains(".")) {
            int index=a.lastIndexOf(".");
            b=a.substring(index+1);
        }
        //
        window awin=ActNametoWindows.get(b);
        if (awin!=null){
            String[] PERs=awin.getPermissions().split(" ");
            Set<String> quanxian=new HashSet<>();
            for (int i=0;i<PERs.length;i++){
                if(PERs[i].contains("android")){
                    quanxian.add(PERs[i]);
                }
            }
            if (quanxian.size()>0){
                String[] output=new String[quanxian.size()];
                quanxian.toArray(output);
                return output;
            }else{
                return null;
            }

        }else{
            LogUtil.V().log("——staticInfo——!!!!I con't find tested permissions.");
            return null;


        }


    }

}
