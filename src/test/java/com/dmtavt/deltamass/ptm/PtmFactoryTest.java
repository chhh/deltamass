package com.dmtavt.deltamass.ptm;

import java.util.EnumSet;
import org.junit.Assert;
import org.junit.Test;
import umich.ptm.PtmFactory;
import umich.ptm.PtmFactory.SOURCE;
import umich.ptm.exceptions.ModParsingException;
import umich.ptm.mod.Mods;

public class PtmFactoryTest {

  @Test
  public void ModFactoryTest() throws Exception {
    try {
      Mods modsGen = PtmFactory.getMods(EnumSet.of(SOURCE.GEN));
      Assert.assertTrue(modsGen.size() > 0);
      Mods modsUni = PtmFactory.getMods(EnumSet.of(SOURCE.UNIMOD));
      Assert.assertTrue(modsUni.size() > 0);
    } catch (ModParsingException e) {
      throw new Exception(e);
    }
  }
}
