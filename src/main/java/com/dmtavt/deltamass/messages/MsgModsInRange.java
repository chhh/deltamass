package com.dmtavt.deltamass.messages;

import java.util.List;
import umich.ptm.mod.Mod;

public class MsgModsInRange {

  public final double dmLo;
  public final double dmHi;
  public final List<Mod> mods;

  public MsgModsInRange(double dmLo, double dmHi, List<Mod> mods) {
    this.dmLo = dmLo;
    this.dmHi = dmHi;
    this.mods = mods;
  }

  @Override
  public String toString() {
    String size = mods == null ? "0" : Integer.toString(mods.size());
    return "MsgModsInRange{" +
        "dmLo=" + dmLo +
        ", dmHi=" + dmHi +
        ", modsCount=" + size +
        '}';
  }
}
