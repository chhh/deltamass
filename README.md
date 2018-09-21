# DeltaMass: view and interrogate open search proteomics data


This program will plot mass-differences from a proteomic "Open Search".
An "Open Search" is any search of fragmentation spectra (MS2) against a
protein database when the precursor mass tolerance was set to a high
enough value. E.g. not the usual 5-10-20 ppm, but instead 50-100-200 Da.
The result of such a search is quite different from the regular one, it
might contain a lot of spurious hits, but a lot of hits will still
correspond to reasonable matches.

The differences in mass between the observed value for a precursor and
the theoretical peptide mass from a database might be many Daltons.
Nevertheless, you'll still see lots of similar mass differences, which
should correspond to Post Translational Modifications (PTMs) or
chemical modifications or other artifacts in the data.

After performing an Open Search you might want to identify the
differences to maybe include them into the variable modifications list
of your search engine settings. You might also discover unexpected
artifacts, like chemical derivatives of peptides occurring because of
your sample preparation protocols etc.

This is where this program comes into play.

## Running
Download the **jar** from [Releases](https://github.com/chhh/deltamass/releases/) section.

##### *Command line*
Run `java -jar okde-x.x.jar` to see the help.
There are quite a few options there and it might seem overwhelming at
first, so you might want to try the GUI version first.

##### *GUI*
Run `java -jar OpenSeachKDE.jar --gui` to launch the GUI. You might want to
include a JVM switch `-Xmx2G` (change the number to the amount of gigabytes of
memory you'll allow the process to consume), so the command becomes:  
`java -Xmx2G -jar OpenSeachKDE.jar --gui`

## Input
The program takes **.pep.xml** files as input. It can accept whole directories
and traverse them recursively as well. See `-i` command line option.  
If your _.pep.xml_ files are named differently, use `-ip` or `--in_pattern` to
change the regular expression used for matching file paths.  
**Note**: the regular expression is matched against the whole absolute path for
each file, so you almost always want to start the regex with `.*`. Use double
backslash to escape characters, e.g. `\\.` to match a literal dot.

## Most important settings
 * `-g` or `--gui` - run in GUI mode.
 * `-i` or `--in` input paths, can be a comma separated list (don't use spaces).
 If one of the files is a directory, then it will be traversed recursively and
 all files within will be matched against a regular expression provided in
 the `-ip` option (which defaults to _*.\\\\.pep\\\\.xml_).
 * `-ip` or `--in_pattern` - a regular expression for matching files found in
 paths of `-i`. The default is `.*\\.pep\\.xml`.
 * `-k` or `--kernel` - the kernel to be used in KDE. Default is *EPANECHNIKOV*,
 which is very fast, but the shape of the peaks might suffer a bit depending on
 your sample size. If you want smoother curves try *GAUSS_FAST* instead.
 * `-b` or `--bandwidth` - list of bandwidths to be used for generating KDEs.
 You can think of bandwidth as of the area of influence of each input data
 point. E.g. `-b 0.1,0.001,0.0001` will plot three KDEs in the same plot using
 these three bandwidths.
 * `-hd` or `--h_dynamic` - will automatically estimate the best bandwidth
 at each nominal mass separately. Thus, the are under the whole KDE might not
 be unity anymore, but the plot might be a lot nicer. It also takes more time,
 as the estimation requires looping through data.
 * `-p` or `--peaks` - if peaks should be detected. If KDE is being plotted
 (using `--plot` which is on by default), then peaks will be overlaid over the
 plot. **Note**: peak picking is very parameter dependent. In general it will
 depend on the number of data points (affected by the total data range and the
 sampling rate `-ms`) and most of all on the denoising, specified in
 `--denoise` and `denoisingParams` options. See help for corresponding options
 and just try different parameters for yourself.

## Plot navigation
 * Left Mouse Button = __LMB__, Right Mouse Button = __RMB__.
 * __Zoom__ - `LMB drag to the right`. Zooms in mass axis, Y-axis automatically
 adjusts to fit all peaks. Or use `Mouse Wheel`. Check context menu options to
 change the behavior.
 * __Un-zoom__ - `LMB drag to the left`. Unzooms the mass axis completely, BUT
 keeps the current Y axis zoom. To restore Y-axis zoom as well use `Mouse Wheel`
  once after full zoom out. You can use `Mouse Wheel` to zoom out.
 * __Pan__ - `Hold Ctrl + LMB drag`. WIll pan X axis only, moving the view left
 and right.
 * `Mouse Wheel` - __zoom__ in/out onto the X point currently under cursor.
 * `LMB click` - __Selection__. Places  two vertical guides to denote the region
 currently displayed in the Info Window. The Info Window will list known
 modifications in this range of masses and also list peptides from your search
 results that fall into this range.
 * `RMB click` - options for the plot (__Preferences__), you can also save the
 plot from here (prefer saving to _PNG_) and change how zooming works.


## Starting tips
 * First do a preliminary run with low mass step (e.g. `-ms 0.01`) and low
 bandwidth (e.g. `-b 0.01` or `List of bandwidths` in GUI) to get a bird's eye
 view of your data.
 * Typically you'd want to look at the data using `-ms 0.0001 -b 0.001` to
 see individual peaks better.
 * The peak at zero is always the largest one, and the situation to the left
 (negative mass shifts) and right (positive) of it are quite different, so it's
 better to view them separately and avoid the zero peak at all.
 * Most of your data points will be concentrated in the zero peak,
 calculating KDE for it is relatively expensive and there is little merit to it.
 You can easily avoid the peak at zero at all by adding e.g. `-ml 0.75` option
 (`Mass Lo` in GUI), which will only plot mass differences above mass of 0.75.
 * Similarly for negative massses, use `-mh -0.75` (`Mass Hi` in GUI) to plot
 everything from negative infinity up to -0.75.
 * Use `-ml` and `-mh` options simultaneously to limit the view and increase
 plot's responsiveness.
 * You can output the detected peaks to a file using `-o` option and skip
 plotting completely by providing `--plot false`, however you'll likely
 want to do the other thing around most of the time.

## Filtering data
Input data can be filtered according to the scores in _pep.xml_ files. All then
input files must have this corr in order for it to work. E.g. if you want
to filter based on _peptideprophet_ probability, all the files must have between
processed with PeptideProphet.  

* Filters are provided using `-s` or `--corr` option. The GUI provides a
builder for the filter strings.  
* The names of the scores are exactly as they appear  _pep.xml_ (e.g. `expect`,
`hyperscore`, `interprophet`, etc). The GUI has a dropdown menu for the most
common ones.  
* Filters can be combined using commas.  
* Supported operators are `>`, `>=`, `==`, `<=`, `<`.

Example: `-s "expect<=0.001,peptideprophet>0.95"`

## Caching
The input data are _.pep.xml_ files, which are typically large, the included
parser for them is not very fast, so if you want to play around with parameters
it makes a lot of sense to cache the parsing results. The option to cache
.pep.xml contents is on by default in both GUI and command line versions.
##### *Disabling*
The cache files are stored nearby your original files. If for some reason you
don't want those extra files to be created use `--cache false` option or untick
`Use cache` checkbox in GUI.
##### *Deleting*
If you've already run the program with cache option and now want to get rid of
those extra files, run the program with `-x` or `--delete_cache` switch, This
won't trigger any processing and will only look for cache files in the input
paths (`-i`) and delete them. The GUI has a `Delete cache` checkbox in *Files &
Cache* section.
