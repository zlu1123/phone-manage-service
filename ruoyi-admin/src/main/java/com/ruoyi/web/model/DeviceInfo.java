package com.ruoyi.web.model;

public class DeviceInfo {
        private String IMEI;
        private String IMEI2;
        private String SN;

        public DeviceInfo(String imei, String imei2, String sn) {
            this.IMEI = imei;
            this.IMEI2 = imei2;
            this.SN = sn;
        }

        public String getIMEI() { return IMEI; }
        public String getIMEI2() { return IMEI2; }
        public String getSN() { return SN; }

        @Override
        public String toString() {
            return "{\n  \"IMEI\": \"" + IMEI + "\",\n" +
                    "  \"IMEI2\": \"" + IMEI2 + "\",\n" +
                    "  \"SN\": \"" + SN + "\"\n}";
        }
    }