package com.dmtavt.deltamass.messages;

public class MsgVersionUpdateInfo {
  public final String programName;
  public final String currentVersion;
  public final String newVersion;
  public final String downloadUrl;

  public MsgVersionUpdateInfo(String programName, String currentVersion, String newVersion,
      String downloadUrl) {
    this.programName = programName;
    this.currentVersion = currentVersion;
    this.newVersion = newVersion;
    this.downloadUrl = downloadUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MsgVersionUpdateInfo that = (MsgVersionUpdateInfo) o;

    if (!programName.equals(that.programName)) {
      return false;
    }
    if (!currentVersion.equals(that.currentVersion)) {
      return false;
    }
    if (!newVersion.equals(that.newVersion)) {
      return false;
    }
    return downloadUrl != null ? downloadUrl.equals(that.downloadUrl) : that.downloadUrl == null;
  }

  @Override
  public int hashCode() {
    int result = programName.hashCode();
    result = 31 * result + currentVersion.hashCode();
    result = 31 * result + newVersion.hashCode();
    result = 31 * result + (downloadUrl != null ? downloadUrl.hashCode() : 0);
    return result;
  }
}
