package io.muse.lineregionfix;

/**
 * Region presets shared by the settings UI and the hook.
 * Each region: display name, ISO country code, timezone ID, locale (lang_country),
 * and a list of carriers (display alpha name + MCC/MNC numeric).
 */
public final class RegionPresets {

    public static final class Carrier {
        public final String alpha;
        public final String numeric;

        public Carrier(String alpha, String numeric) {
            this.alpha = alpha;
            this.numeric = numeric;
        }
    }

    public static final class Region {
        public final String name;
        public final String iso;
        public final String timezoneId;
        public final String localeTag; // e.g. ja_JP
        public final Carrier[] carriers;

        public Region(String name, String iso, String timezoneId, String localeTag, Carrier[] carriers) {
            this.name = name;
            this.iso = iso;
            this.timezoneId = timezoneId;
            this.localeTag = localeTag;
            this.carriers = carriers;
        }
    }

    public static final Region[] REGIONS = {
            new Region("日本", "jp", "Asia/Tokyo", "ja_JP", new Carrier[]{
                    new Carrier("NTT DOCOMO", "44010"),
                    new Carrier("au (KDDI)", "44007"),
                    new Carrier("SoftBank", "44020"),
                    new Carrier("Rakuten Mobile", "44011"),
            }),
            new Region("韩国", "kr", "Asia/Seoul", "ko_KR", new Carrier[]{
                    new Carrier("SK Telecom", "45005"),
                    new Carrier("KT", "45002"),
                    new Carrier("LG U+", "45006"),
            }),
            new Region("台湾", "tw", "Asia/Taipei", "zh_TW", new Carrier[]{
                    new Carrier("中华电信", "46692"),
                    new Carrier("台湾大哥大", "46697"),
                    new Carrier("远传电信", "46601"),
            }),
            new Region("香港", "hk", "Asia/Hong_Kong", "zh_HK", new Carrier[]{
                    new Carrier("CSL", "45400"),
                    new Carrier("3HK", "45403"),
                    new Carrier("SmarTone", "45406"),
                    new Carrier("中国移动香港", "45412"),
            }),
            new Region("泰国", "th", "Asia/Bangkok", "th_TH", new Carrier[]{
                    new Carrier("AIS", "52003"),
                    new Carrier("TrueMove H", "52000"),
                    new Carrier("dtac", "52005"),
            }),
            new Region("美国", "us", "America/New_York", "en_US", new Carrier[]{
                    new Carrier("T-Mobile", "310260"),
                    new Carrier("AT&T", "310410"),
                    new Carrier("Verizon", "311480"),
            }),
            new Region("德国", "de", "Europe/Berlin", "de_DE", new Carrier[]{
                    new Carrier("Telekom", "26201"),
                    new Carrier("Vodafone", "26202"),
                    new Carrier("O2", "26207"),
            }),
    };

    public static Region byIso(String iso) {
        if (iso != null) {
            for (Region r : REGIONS) {
                if (r.iso.equals(iso)) {
                    return r;
                }
            }
        }
        return REGIONS[0];
    }

    private RegionPresets() {
    }
}
