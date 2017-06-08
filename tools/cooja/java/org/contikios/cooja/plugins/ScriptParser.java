/*
 * Copyright (c) 2008, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

package org.contikios.cooja.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.apache.log4j.Logger;

import org.contikios.cooja.Simulation;

// tete_begin
import java.util.Random;
import org.contikios.cooja.Mote;
import java.util.ArrayList;

import org.contikios.cooja.mspmote.MspMote;
import org.contikios.cooja.mspmote.plugins.MspCLI;
import org.contikios.cooja.Cooja;
//tete_end

public class ScriptParser {
  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(ScriptParser.class);

  private long timeoutTime = -1;
  private String timeoutCode = null;

  private String code = null;

  //tete_begin
    /**
     * parse script to start up CLI.
     */

    static public MspCLI pluginClass = null;
    static public Object pluginClasses = null;
    static public Cooja cooja = null;
    public ArrayList<MspCLI> mspclis = new ArrayList<MspCLI>();
    private static Simulation simulation = null;
  //tete_end

  public ScriptParser(String code) throws ScriptSyntaxErrorException {

    code = fixNewlines(code);

    code = stripMultiLineComments(code);

    code = stripSingleLineComments(code);

    //tete_begin
    code = replaceRandom(code);
    code = parseMspCLIWithAction(code);
    //tete_end

    code = parseTimeout(code);

    code = parseTimeoutWithAction(code);

    code = replaceYieldThenWaitUntils(code);

    code = replaceYields(code);

    code = replaceWaitUntils(code);

    this.code = code;
  }

  private String fixNewlines(String code) {
    code = code.replaceAll("\r\n", "\n");
    code = "\n" + code + "\n";
    return code;
  }

  private String stripSingleLineComments(String code) {
    /* TODO Handle strings */
    Pattern pattern = Pattern.compile("//.*\n");
    Matcher matcher = pattern.matcher(code);
    code = matcher.replaceAll("\n");
    return code;
  }

  private String stripFirstComment(String code) {
    int first = code.indexOf('"');
    if (first < 0) {
      return code;
    }
    int second = code.indexOf('"', first+1);

    code = code.substring(0, first) + code.substring(second+1, code.length());
    return code;
  }

  private String stripMultiLineComments(String code) {
    /* TODO Handle strings */
    Pattern pattern =
      Pattern.compile("/\\*([^*]|[\n]|(\\*+([^*/]|[\n])))*\\*+/");
    Matcher matcher = pattern.matcher(code);

    while (matcher.find()) {
      String match = matcher.group();
      int newLines = match.split("\n").length;
      String replacement = "";
      for (int i=0; i < newLines; i++) {
        replacement += "\n";
      }
      code = matcher.replaceFirst(replacement);
      matcher.reset(code);
    }
    return code;
  }

//tete_begin
// part of MspCLI
// 从ScriptRuner获得simulation对象,可以从该对象获得所有仿真节点
  public static void setSimulation(Simulation sim){
      ScriptParser.simulation = sim;
  }

  private String parseMspCLIWithAction(String code) throws ScriptSyntaxErrorException{
    // 用来存储多条指令
    ArrayList<String[]> decr = new ArrayList<String[]>();

    Pattern pattern = Pattern.compile("MspCLI\\((.*?)\\)");
    Matcher matcher = pattern.matcher(code);
    int i = 0;
    while(matcher.find()){
       i++;
       String[] tmp = matcher.group(1).split(",");
       // 命令只有两个参数
       if(tmp.length != 2)
         throw new IllegalArgumentException("The " + i + "'th of @MspCLI(number, command) should have only two arguments.");
       tmp[0] = tmp[0].trim();
       tmp[1] = tmp[1].trim();
       decr.add(tmp);
    }
    // 执行MspCLI
    launchMspCLI(decr);
    matcher.reset(code);
    code = code.replaceAll("MspCLI\\((.*?)\\)", ";");
    return code;
  }

  private void launchMspCLI(ArrayList<String[]> cmd){
	  int moteNum = 0;

    if (cmd.size() == 0)
      return;


    for(String[] str : cmd){
        int index = 0;
        for (Object mote: this.simulation.getMotes()) {
            //判断是否为命令参数指定的节点
            if(++index == Integer.parseInt(str[0])) {
                ((MspMote)mote).executeCLICommand(str[1]);
                System.out.println("Start up " + str[0] + "'th and command is <" + str[1] + ">");
            }
        }
    }

  }
//tete_end

//tete_begin
// part of reverse function and kill function
/*
 *  递归的将代码中的随机函数替换为随机数
 */
  private static String replaceRandom(String code){
    String randStrRegex = "rand\\s*?\\(([0-9\\s]+),([0-9\\s]+)\\)";
    Pattern p = Pattern.compile(randStrRegex);
    Matcher m = p.matcher(code);
    if(m.find())
      code =  code.replaceFirst(
              randStrRegex,
              // 获得代码中随机函数参数，并生成随机数
              rand(Integer.parseInt(m.group(1).trim()), Integer.parseInt(m.group(2).trim()))+"");
    //System.out.println("Log: " + code);
    Pattern pattern = Pattern.compile(randStrRegex);
    Matcher match = pattern.matcher(code);
    if(match.find())
      code = replaceRandom(code);
    return code;
  }

  /**
  * 解析reverse函数
  * reverse函数执行翻转节点状态的功能
  * @param code
  * @param storage        每个节点的节点编号和执行翻转动作的时间组成长度为2的节点信息数组，将
  *                       所有节点的信息数组存储进storage。
  * @return 去掉reverse函数之后的代码
  */
 private static String parseReverse(String code, ArrayList<String[]> storage){

     code = replaceRandom(code);
     String regex = "reverse\\s*?\\(([0-9\\s]+),([0-9\\s]+)\\)";
     Pattern pattern = Pattern.compile(regex);
     Matcher match = pattern.matcher(code);

     while(match.find()){
//                        有时间给这些println函数都弄成日志输出
//                        System.out.println(match.group(1) + ": " + match.group(2));
             storage.add(new String[]{match.group(1),  match.group(2)});
     }

     return code.replaceAll(regex, ";");
 }

 /**
  * 解析kill节点函数(解析起来和reverse一毛一样)
  * kill函数杀死节点
  * @param code
  * @param storage        每个节点的节点编号和执行死亡动作的时间组成长度为2的节点信息数组，将
  *                                         所有节点的信息数组存储进storage。
  * @return 去掉kill函数之后的代码
  */
 private static String parseKill(String code, ArrayList<String[]> storage){

     code = replaceRandom(code);
     String regex = "kill\\s*?\\(([0-9\\s]+),([0-9\\s]+)\\)";
     Pattern pattern = Pattern.compile(regex);
     Matcher match = pattern.matcher(code);

     while(match.find()){
//                        有时间给这些println函数都弄成日志输出
//                        System.out.println(match.group(1) + ": " + match.group(2));
      storage.add(new String[]{match.group(1),  match.group(2)});
     }

     return code.replaceAll(regex, ";");
 }


 /**
  * 获得随机数
  * @param min
  * @param max
  * @return [min, max)范围内的随机数
  */
 private static int rand(int min, int max){
         return new Random().nextInt(max)%(max-min) + min;
 }
// tete_end

  private String parseTimeout(String code) throws ScriptSyntaxErrorException {
    Pattern pattern = Pattern.compile(
        "TIMEOUT\\(" +
        "([0-9]+)" /* timeout */ +
        "\\)"
    );
    Matcher matcher = pattern.matcher(code);

    if (!matcher.find()) {
      return code;
    }

    if (timeoutTime > 0) {
      throw new ScriptSyntaxErrorException("Only one timeout handler allowed");
    }

    timeoutTime = Long.parseLong(matcher.group(1))*Simulation.MILLISECOND;
    timeoutCode = ";";
    matcher.reset(code);
    code = matcher.replaceFirst(";");
    matcher.reset(code);

    if (matcher.find()) {
      throw new ScriptSyntaxErrorException("Only one timeout handler allowed");
    }
    return code;
  }

  private String parseTimeoutWithAction(String code) throws ScriptSyntaxErrorException {
    Pattern pattern = Pattern.compile(
        "TIMEOUT\\(" +
        "([0-9]+)" /* timeout */ +
        "[\\s]*,[\\s]*" +
        "(.*)" /* code */ +
        "\\)"
    );
    Matcher matcher = pattern.matcher(code);

    if (!matcher.find()) {
      return code;
    }

    if (timeoutTime > 0) {
      throw new ScriptSyntaxErrorException("Only one timeout handler allowed");
    }
    timeoutTime = Long.parseLong(matcher.group(1))*Simulation.MILLISECOND;
    timeoutCode = matcher.group(2);
    matcher.reset(code);
    code = matcher.replaceFirst(";");
    matcher.reset(code);
    if (matcher.find()) {
      throw new ScriptSyntaxErrorException("Only one timeout handler allowed");
    }
    return code;
  }

  private String replaceYields(String code) throws ScriptSyntaxErrorException {
    Pattern pattern = Pattern.compile(
        "YIELD\\(\\)"
    );
    return pattern.matcher(code).replaceAll("SCRIPT_SWITCH()");
  }

  private String replaceYieldThenWaitUntils(String code) throws ScriptSyntaxErrorException {
    Pattern pattern = Pattern.compile(
        "YIELD_THEN_WAIT_UNTIL\\(" +
        "(.*)" /* expression */ +
        "\\)"
    );
    Matcher matcher = pattern.matcher(code);

    while (matcher.find()) {
      code = matcher.replaceFirst(
          "YIELD(); WAIT_UNTIL(" + matcher.group(1) + ")");
      matcher.reset(code);
    }

    return code;
  }

  private String replaceWaitUntils(String code) throws ScriptSyntaxErrorException {
    Pattern pattern = Pattern.compile(
        "WAIT_UNTIL\\(" +
        "(.*)" /* expression */ +
        "\\)"
    );
    Matcher matcher = pattern.matcher(code);

    while (matcher.find()) {
      code = matcher.replaceFirst(
          "while (!(" + matcher.group(1) + ")) { " +
          " SCRIPT_SWITCH(); " +
      "}");
      matcher.reset(code);
    }

    return code;
  }

  public String getJSCode() {
    return getJSCode(code, timeoutCode);
  }

  public static String getJSCode(String code, String timeoutCode) {
    return
    "timeout_function = null; " +
    "function run() { " +
    "SEMAPHORE_SIM.acquire(); " +
    "SEMAPHORE_SCRIPT.acquire(); " + /* STARTUP BLOCKS HERE! */
    "if (SHUTDOWN) { SCRIPT_KILL(); } " +
    "if (TIMEOUT) { SCRIPT_TIMEOUT(); } " +
    "msg = new java.lang.String(msg); " +
    "node.setMoteMsg(mote, msg); " +
    code +
    "\n" +
    "\n" +
    "while (true) { SCRIPT_SWITCH(); } " /* SCRIPT ENDED */+
    "};" +
    "\n" +
    "function GENERATE_MSG(time, msg) { " +
    " log.generateMessage(time, msg); " +
    "};\n" +
    "\n" +
    "function SCRIPT_KILL() { " +
    " SEMAPHORE_SIM.release(100); " +
    " throw('test script killed'); " +
    "};\n" +
    "\n" +
    "function SCRIPT_TIMEOUT() { " +
    timeoutCode + "; " +
    " if (timeout_function != null) { timeout_function(); } " +
    " log.log('TEST TIMEOUT\\n'); " +
    " log.testFailed(); " +
    " while (!SHUTDOWN) { " +
    "  SEMAPHORE_SIM.release(); " +
    "  SEMAPHORE_SCRIPT.acquire(); " /* SWITCH BLOCKS HERE! */ +
    " } " +
    " SCRIPT_KILL(); " +
    "};\n" +
    "\n" +
    "function SCRIPT_SWITCH() { " +
    " SEMAPHORE_SIM.release(); " +
    " SEMAPHORE_SCRIPT.acquire(); " /* SWITCH BLOCKS HERE! */ +
    " if (SHUTDOWN) { SCRIPT_KILL(); } " +
    " if (TIMEOUT) { SCRIPT_TIMEOUT(); } " +
    " msg = new java.lang.String(msg); " +
    " node.setMoteMsg(mote, msg); " +
    "};\n" +
    "\n" +
    "function write(mote,msg) { " +
    " mote.getInterfaces().getLog().writeString(msg); " +
    "};\n";
  }

  public long getTimeoutTime() {
    return timeoutTime;
  }

  public class ScriptSyntaxErrorException extends ScriptException {
    public ScriptSyntaxErrorException(String msg) {
      super(msg);
    }
  }

}
