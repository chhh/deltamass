package com.dmtavt.deltamass.messages;

import com.dmtavt.deltamass.logic.LogicKde.SpmInfo;
import java.util.List;

public class MsgPsmsResponse {
  public final List<SpmInfo> spmInfos;

  public MsgPsmsResponse(List<SpmInfo> spmInfos) {
    this.spmInfos = spmInfos;
  }
}
