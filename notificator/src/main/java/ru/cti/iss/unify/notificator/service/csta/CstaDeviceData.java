package ru.cti.iss.unify.notificator.service.csta;

import com.sen.openscape.csta.callcontrol.CstaDevice;

public class CstaDeviceData {
    public final CstaDevice device;
    public final String crossRefId;

    public CstaDeviceData(CstaDevice device, String crossRefId) {
        this.device = device;
        this.crossRefId = crossRefId;
    }
}
