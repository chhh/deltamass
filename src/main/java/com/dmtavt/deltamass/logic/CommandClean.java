package com.dmtavt.deltamass.logic;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.dmtavt.deltamass.args.converters.ExistingReadablePathValidator;
import com.dmtavt.deltamass.args.converters.PathConverter;
import com.dmtavt.deltamass.args.converters.StringToPathValidator;
import java.nio.file.Path;
import java.util.List;

@Parameters(commandDescription = "Clean up cached files.",
    commandNames = {CommandClean.CMD})
public class CommandClean {

  public static final String CMD = "clean";

  @Parameter(names = {"-h", "--help"}, hidden = true, help = true, description = "Display help message.")
  boolean help = false;

  @Parameter(names = {"-i", "--in"}, required = true,
    description = "Relative or absolute paths to directories or separate files. Separate entries with commas.",
    validateWith = StringToPathValidator.class, converter = PathConverter.class,
    validateValueWith = ExistingReadablePathValidator.class,
    order = 10)
  public List<Path> inputFiles;

  @Parameter(names = {"--dry-run"}, description = "Print files to be deleted without taking action.",
    order = 20)
  public boolean dryRun = false;

  @Parameter(names = {"--no-pep"}, description = "Do not clean up cached peptide identification files.",
    order = 30)
  public boolean noPep = false;

  @Parameter(names = {"--no-lcms"}, description = "Do not clean up LC-MS mass calibration files.",
  order = 40)
  public boolean noLcms = false;


}
