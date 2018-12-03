package com.dmtavt.deltamass;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.ParameterException;
import com.dmtavt.deltamass.logic.CommandClean;
import com.dmtavt.deltamass.logic.CommandGui;
import com.dmtavt.deltamass.logic.CommandPeaks;
import com.dmtavt.deltamass.logic.CommandPlot;
import com.dmtavt.deltamass.logic.LogicClean;
import com.dmtavt.deltamass.logic.LogicGui;
import com.dmtavt.deltamass.logic.LogicPeaks;
import com.dmtavt.deltamass.logic.LogicPlot;
import com.dmtavt.deltamass.utils.TextUtils;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaMassMain {

  private static final Logger log = LoggerFactory.getLogger(DeltaMassMain.class);

  public static void main(String[] args) {
    System.out.println(DeltaMassInfo.getNameVersion());
    System.out.println();

    parseArgs(args);
  }

  private static void parseArgs(String[] args) {

    CommandGui cmdGui = new CommandGui();
    CommandPeaks cmdPeaks = new CommandPeaks();
    CommandPlot cmdPlot = new CommandPlot();
    CommandClean cmdCleanup = new CommandClean();

    JCommander jc = JCommander.newBuilder()
        .acceptUnknownOptions(false)
        .allowAbbreviatedOptions(false)
        .programName("java -jar " + DeltaMassInfo.Name + "_" + DeltaMassInfo.Ver + ".jar")
        .addCommand(cmdGui)
        .addCommand(cmdPeaks)
        .addCommand(cmdPlot)
        .addCommand(cmdCleanup)
        .build();

    if (args.length == 0) {
      System.out.println("No command found.");
    }

    if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
      String msg = "Available commands:\n" + getWrappedCommandsDescription(jc).toString();
      System.out.println(msg);
      return;
    }

    try {
      jc.parse(args);
    } catch (MissingCommandException e) {
      String msg = "Error parsing command line parameters, unknown command '" +
          e.getUnknownCommand() + "'.\n\n" +
          "Available commands:\n" + getWrappedCommandsDescription(jc).toString();
      System.err.println(msg);
      return;
    } catch (ParameterException e) {
      System.err.printf("Error parsing parameters for command '%s'.\n%s",
          e.getJCommander().getParsedCommand(), e.getMessage());
      return;
    }

    // some known command has been parsed
    if (Arrays.stream(args).anyMatch(s -> "-h".equals(s) || "--help".equals(s))) {
      // show help for command
      String cmd = "Command: " + jc.getParsedCommand();
      System.out.println(cmd);
      System.out.println(TextUtils.repeat("-", cmd.length()));
      jc.usage(jc.getParsedCommand());
      return;
    }

    // do something with the parsed arguments
    System.out.println("Selected command: " + jc.getParsedCommand());
    switch (jc.getParsedCommand()) {
      case CommandGui.CMD:
        new LogicGui(cmdGui).run();
        break;
      case CommandPeaks.CMD:
        new LogicPeaks(cmdPeaks).run();
        break;
      case CommandPlot.CMD:
        new LogicPlot(cmdPlot).run();
        break;
      case CommandClean.CMD:
        new LogicClean(cmdCleanup).run();
        break;
      default:
        throw new IllegalStateException("Unknown command parsed: " + jc.getParsedCommand());
    }
  }

  private static StringBuilder getWrappedCommandsDescription(JCommander jc) {
    final int indent = 4;
    final int maxCmd = jc.getCommands().keySet().stream().mapToInt(String::length).max().orElse(0);
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, JCommander> e : jc.getCommands().entrySet()) {
      String cmd = e.getKey();
      String desc = "\n" + TextUtils.repeat(" ", indent) + cmd +
          TextUtils.repeat(" ", maxCmd - cmd.length()) +
          " - " + jc.getCommandDescription(cmd) + "\n";
      StringBuilder wrapped = new StringBuilder();
      TextUtils.wrap(wrapped, jc.getColumnSize(), 8, desc);
      sb.append(wrapped.toString());
    }
    sb.append("\nTry '").append(DeltaMassInfo.Name).append(" <command-name> -h' for more info.\n");
    return sb;
  }


}
