package com.ffxivcensus.gatherer.player;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ffxivcensus.gatherer.lodestone.CharacterDeletedException;
import com.ffxivcensus.gatherer.lodestone.LodestonePageLoader;
import com.ffxivcensus.gatherer.lodestone.ProductionLodestonePageLoader;
import com.ffxivcensus.gatherer.task.GathererTask;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

/**
 * Builder class for creating PlayerBean objects from the Lodestone.
 *
 * @author Peter Reid
 * @author Matthew Hillier
 * @since v1.0
 * @see GathererTask
 */
@Service
public class PlayerBuilder {

    private static final String HEADER_LAST_MODIFIED = "Last-Modified";
    private static final String ATTR_SRC = "src";
    private static final String TAG_IMG = "img";
    private static final String TAG_A = "a";
    private static final String LAYOUT_CHARACTER_DETAIL_IMAGE = "character__detail__image";
    private static final String LAYOUT_CHARACTER_MOUNTS = "character__mounts";
    private static final String TAG_LI = "li";
    private static final String LAYOUT_DATA_TOOLTIP = "data-tooltip";
    private static final String TAG_DIV = "div";
    private static final String LAYOUT_CHARACTER_MINION = "character__minion";
    private static final String LAYOUT_CHARACTER_JOB_LEVEL = "character__job__level";
    private static final String LAYOUT_CHARACTER_FREECOMPANY_NAME = "character__freecompany__name";
    private static final String LAYOUT_CHARACTER_BLOCK_BOX = "character-block__box";
    private static final String LAYOUT_FRAME_CHARA_WORLD = "frame__chara__world";
    private static final String LAYOUT_CHARACTER_BLOCK_NAME = "character-block__name";
    private static final Logger LOG = LoggerFactory.getLogger(PlayerBuilder.class);
    /**
     * Number of days inactivity before character is considered inactive
     */
    private static final int ACTIVITY_RANGE_DAYS = 30;

    private static final long ONE_DAY_IN_MILLIS = 86400000;

    private LodestonePageLoader pageLoader = new ProductionLodestonePageLoader();

    /**
     * Set player class levels.
     * As of 4.0, this is now parsed in the order:
     * - Gladiator
     * - Marauder
     * - Dark Knight
     * - Monk
     * - Dragoon
     * - Ninja
     * - Samurai
     * - White Mage
     * - Scholar
     * - Astrologian
     * - Bard
     * - Machinist
     * - Black Mage
     * - Summoner
     * - Red Mage
     *
     * @param arrLevels integer array of classes in order displayed on lodestone.
     */
    public void setLevels(final PlayerBean player, final int[] arrLevels) {
        player.setLevelGladiator(arrLevels[0]);
        player.setLevelMarauder(arrLevels[1]);
        player.setLevelDarkknight(arrLevels[2]);
        player.setLevelPugilist(arrLevels[3]);
        player.setLevelLancer(arrLevels[4]);
        player.setLevelRogue(arrLevels[5]);
        player.setLevelSamurai(arrLevels[6]);
        player.setLevelConjurer(arrLevels[7]);
        player.setLevelScholar(arrLevels[8]);
        player.setLevelAstrologian(arrLevels[9]);
        player.setLevelArcher(arrLevels[10]);
        player.setLevelMachinist(arrLevels[11]);
        player.setLevelThaumaturge(arrLevels[12]);
        player.setLevelArcanist(arrLevels[13]);
        player.setLevelRedmage(arrLevels[14]);
        player.setLevelBluemage(arrLevels[15]);
        player.setLevelCarpenter(arrLevels[16]);
        player.setLevelBlacksmith(arrLevels[17]);
        player.setLevelArmorer(arrLevels[18]);
        player.setLevelGoldsmith(arrLevels[19]);
        player.setLevelLeatherworker(arrLevels[20]);
        player.setLevelWeaver(arrLevels[21]);
        player.setLevelAlchemist(arrLevels[22]);
        player.setLevelCulinarian(arrLevels[23]);
        player.setLevelMiner(arrLevels[24]);
        player.setLevelBotanist(arrLevels[25]);
        player.setLevelFisher(arrLevels[26]);
        // Elemental Level is an optional component at the end of the section, so may not exist
        if(arrLevels.length > 27) {
            player.setLevelEureka(arrLevels[27]);
        }
    }

    /**
     * Determine if a player has a specified mount
     *
     * @param mountName the name of the mount to check for.
     * @return whether the player has the specified mount.
     */
    public boolean doesPlayerHaveMount(final PlayerBean player, final String mountName) {
        return player.getMounts().contains(mountName);
    }

    /**
     * Determine if a player has a specified minion.
     *
     * @param minionName the name of the minion to check for
     * @return whether the player has the specified minion.
     */
    public boolean doesPlayerHaveMinion(final PlayerBean player, final String minionName) {
        return player.getMinions().contains(minionName);
    }

    /**
     * Fetch a player from the lodestone specified by ID.
     *
     * @param playerID the ID of the player to fetch
     * @param attempt the number of times this character has been attempted.
     * @return the player object matching the specified ID.
     * @throws Exception exception thrown if more class levels returned than anticipated.
     */
    public PlayerBean getPlayer(final int playerID) throws IOException, InterruptedException {
        // Initialize player object to return
        PlayerBean player = new PlayerBean();
        player.setId(playerID);
        // Declare HTML document
        try {
            Document doc = pageLoader.getCharacterPage(playerID);

            player.setPlayerName(getNameFromPage(doc));
            player.setRealm(getRealmFromPage(doc));
            player.setRace(getRaceFromPage(doc));
            player.setGender(getGenderFromPage(doc));
            player.setGrandCompany(getGrandCompanyFromPage(doc));
            player.setFreeCompany(getFreeCompanyFromPage(doc));
            player.setDateImgLastModified(getDateLastUpdatedFromPage(doc, playerID));
            setLevels(player, getLevelsFromPage(doc));
            player.setMounts(getMountsFromPage(doc));
            player.setMinions(getMinionsFromPage(doc));
            player.setHas30DaysSub(doesPlayerHaveMinion(player, "Wind-up Cursor"));
            player.setHas60DaysSub(doesPlayerHaveMinion(player, "Black Chocobo Chick"));
            player.setHas90DaysSub(doesPlayerHaveMinion(player, "Beady Eye"));
            player.setHas180DaysSub(doesPlayerHaveMinion(player, "Minion Of Light"));
            player.setHas270DaysSub(doesPlayerHaveMinion(player, "Wind-up Leader"));
            player.setHas360DaysSub(doesPlayerHaveMinion(player, "Wind-up Odin"));
            player.setHas450DaysSub(doesPlayerHaveMinion(player, "Wind-up Goblin"));
            player.setHas630DaysSub(doesPlayerHaveMinion(player, "Wind-up Nanamo"));
            player.setHas960DaysSub(doesPlayerHaveMinion(player, "Wind-up Firion"));
            player.setHasPreOrderArr(doesPlayerHaveMinion(player, "Cait Sith Doll"));
            player.setHasPreOrderHW(doesPlayerHaveMinion(player, "Chocobo Chick Courier"));
            player.setHasPreOrderSB(doesPlayerHaveMinion(player, "Wind-up Red Mage"));
            player.setHasPreOrderShB(doesPlayerHaveMinion(player, "Baby Gremlin"));
            player.setHasARRArtbook(doesPlayerHaveMinion(player, "Model Enterprise"));
            player.setHasHWArtbookOne(doesPlayerHaveMinion(player, "Wind-Up Relm"));
            player.setHasHWArtbookTwo(doesPlayerHaveMinion(player, "Wind-Up Hraesvelgr"));
            player.setHasSBArtbook(doesPlayerHaveMinion(player, "Wind-up Yotsuyu"));
            player.setHasSBArtbookTwo(doesPlayerHaveMinion(player, "Dress-up Tataru"));
            player.setHasEncyclopediaEorzea(doesPlayerHaveMinion(player, "Namingway"));
            player.setHasBeforeMeteor(doesPlayerHaveMinion(player, "Wind-up Dalamud"));
            player.setHasBeforeTheFall(doesPlayerHaveMinion(player, "Set Of Primogs"));
            player.setHasSoundtrack(doesPlayerHaveMinion(player, "Wind-up Bahamut"));
            player.setHasAttendedEternalBond(doesPlayerHaveMinion(player, "Demon Box"));
            player.setHasCompletedHWSightseeing(doesPlayerHaveMinion(player, "Fledgling Apkallu"));
            player.setHasCompleted2pt5(doesPlayerHaveMinion(player, "Midgardsormr"));
            player.setHasFiftyComms(doesPlayerHaveMinion(player, "Princely Hatchling"));
            player.setHasMooglePlush(doesPlayerHaveMinion(player, "Wind-up Delivery Moogle"));
            player.setHasTopazCarbunclePlush(doesPlayerHaveMinion(player, "Heliodor Carbuncle"));
            player.setHasEmeraldCarbunclePlush(doesPlayerHaveMinion(player, "Peridot Carbuncle"));
            player.setHasCompletedHildibrand(doesPlayerHaveMinion(player, "Wind-up Gentleman"));
            player.setHasPS4Collectors(doesPlayerHaveMinion(player, "Wind-up Moogle"));
            player.setHasCompleted3pt1(doesPlayerHaveMinion(player, "Wind-up Haurchefant"));
            player.setHasCompleted3pt3(doesPlayerHaveMinion(player, "Wind-up Aymeric"));
            player.setHasEternalBond(doesPlayerHaveMount(player, "Ceremony Chocobo"));
            player.setHasARRCollectors(doesPlayerHaveMount(player, "Coeurl"));
            player.setHasKobold(doesPlayerHaveMount(player, "Bomb Palanquin"));
            player.setHasSahagin(doesPlayerHaveMount(player, "Cavalry Elbst"));
            player.setHasAmaljaa(doesPlayerHaveMount(player, "Cavalry Drake"));
            player.setHasSylph(doesPlayerHaveMount(player, "Laurel Goobbue"));
            player.setHasMoogle(doesPlayerHaveMount(player, "Cloud Mallow"));
            player.setHasVanuVanu(doesPlayerHaveMount(player, "Sanuwa"));
            player.setHasVath(doesPlayerHaveMount(player, "Kongamato"));
            player.setHasCompletedHW(doesPlayerHaveMount(player, "Midgardsormr"));
            // Main Scenario quest doesn't drop a minion, so instead assume players will at least play one of the Level 70 dungeons and
            // eventually get the minion
            player.setHasCompletedSB(doesPlayerHaveMinion(player, "Ivon Coeurlfist Doll") || doesPlayerHaveMinion(player, "Dress-up Yugiri")
                                     || doesPlayerHaveMinion(player, "Wind-up Exdeath"));
            player.setLegacyPlayer(doesPlayerHaveMount(player, "Legacy Chocobo"));
            player.setActive(isPlayerActiveInDateRange(player));
            player.setCharacterStatus(player.isActive() ? CharacterStatus.ACTIVE : CharacterStatus.INACTIVE);
        } catch(CharacterDeletedException cde) {
            player.setCharacterStatus(CharacterStatus.DELETED);
        }
        return player;
    }

    /**
     * Determine whether a player is active based upon the last modified date of their full body image
     *
     * @return whether player has been active inside the activity window
     */
    private boolean isPlayerActiveInDateRange(final PlayerBean player) {

        Calendar date = Calendar.getInstance();
        long t = date.getTimeInMillis();

        Date nowMinusIncludeRange = new Date(t - (ACTIVITY_RANGE_DAYS * ONE_DAY_IN_MILLIS));
        return player.getDateImgLastModified().after(nowMinusIncludeRange); // If the date occurs between the include range and now, then
                                                                            // return true. Else false
    }

    /**
     * Given a lodestone profile page, return the name of the character.
     *
     * @param doc the lodestone profile page.
     * @return the name of the character.
     */
    private String getNameFromPage(final Document doc) {
        String[] parts = doc.title().split(Pattern.quote("|"));
        return parts[0].trim();
    }

    /**
     * Given a lodestone profile page, return the realm of the character.
     *
     * @param doc the lodestone profile page.
     * @return the realm of the character.
     */
    private String getRealmFromPage(final Document doc) {
        // Get elements in the player name area, and return the Realm name (contained in the span)
        return doc.getElementsByClass(LAYOUT_FRAME_CHARA_WORLD).get(0).text().replace("(", "").replace(")", "");
    }

    /**
     * Given a lodestone profile page, return the race of the character.
     *
     * @param doc the lodestone profile page.
     * @return the race of the character.
     */
    private String getRaceFromPage(final Document doc) {
        return doc.getElementsByClass(LAYOUT_CHARACTER_BLOCK_NAME).get(0).textNodes().get(0).text().trim();
    }

    /**
     * Given a lodestone profile page, return the gender of the character.
     *
     * @param doc the lodestone profile page.
     * @return the gender of the character.
     */
    private String getGenderFromPage(final Document doc) {
        String[] parts = doc.getElementsByClass(LAYOUT_CHARACTER_BLOCK_NAME).get(0).text().split(Pattern.quote("/"));
        String gender = parts[1].trim();
        if(gender.equals("♂")) {
            return "male";
        } else if(gender.equals("♀")) {
            return "female";
        } else {
            return null;
        }
    }

    /**
     * Given a lodestone profile page, return the grand company of the character.
     *
     * @param doc the lodestone profile page.
     * @return the grand company of the character.
     */
    private String getGrandCompanyFromPage(final Document doc) {
        String gc = null;
        // Get all elements with class chara_profile_box_info
        Elements elements = doc.getElementsByClass(LAYOUT_CHARACTER_BLOCK_BOX);
        if(elements.size() == 5) {
            gc = elements.get(3).getElementsByClass(LAYOUT_CHARACTER_BLOCK_NAME).get(0).text().split("/")[0].trim();
        } else if(elements.size() == 4) {
            if(!elements.get(3).getElementsByClass(LAYOUT_CHARACTER_FREECOMPANY_NAME).isEmpty()) { // If box is fc
                gc = "none";
            } else {
                gc = elements.get(3).getElementsByClass(LAYOUT_CHARACTER_BLOCK_NAME).get(0).text().split("/")[0].trim();
            }
        } else {
            gc = "none";
        }

        return gc;
    }

    /**
     * Given a lodestone profile page, return the free company of the character.
     *
     * @param doc the lodestone profile page.
     * @return the free company of the character.
     */
    private String getFreeCompanyFromPage(final Document doc) {
        String fc = null;
        // Get all elements with class chara_profile_box_info
        Elements elements = doc.getElementsByClass(LAYOUT_CHARACTER_BLOCK_BOX);

        // Checks to see if optional FC has been added
        if(elements.size() == 5) {
            fc = elements.get(4).getElementsByClass(LAYOUT_CHARACTER_FREECOMPANY_NAME).get(0).getElementsByTag(TAG_A).text();
        } else if(elements.size() == 4) { // If only 4 elements present

            if(!elements.get(3).getElementsByClass(LAYOUT_CHARACTER_FREECOMPANY_NAME).isEmpty()) { // If box is fc
                fc = elements.get(3).getElementsByClass(LAYOUT_CHARACTER_FREECOMPANY_NAME).get(0).getElementsByTag(TAG_A).text();
            } else { // Else must not be gc
                fc = "none";
            }
        } else {
            fc = "none";
        }
        return fc;
    }

    /**
     * Given a lodestone profile page, return the levelset of the character.
     *
     * @param doc the lodestone profile page
     * @return the set of levels of the player in the order displayed on the lodestone.
     * @throws Exception Exception thrown if more classes found than anticipated.
     */
    private int[] getLevelsFromPage(final Document doc) {
        // Initialize array list in which to store levels (in order displayed on lodestone)
        List<Integer> levels = new ArrayList<>();

        Element classJobTab = doc.getElementsByClass("character__content").get(2);
        for(Element jobLevel : classJobTab.getElementsByClass(LAYOUT_CHARACTER_JOB_LEVEL)) {
            String strLvl = jobLevel.text();
            if(strLvl.equals("-")) {
                levels.add(0);
            } else {
                levels.add(Integer.parseInt(strLvl));
            }
        }

        // Initialize int array
        int[] arrLevels = new int[levels.size()];
        // Convert array list to array of ints
        for(int index = 0; index < levels.size(); index++) {
            arrLevels[index] = Integer.parseInt(levels.get(index).toString());
        }

        // Check if levels array is larger than this system is programmed for
        // As of 4.5, this is now 28 - SCH and SMN are 2 jobs, + SAM, RDM, BLU & Eureka
        if(arrLevels.length > 28) {
            throw new IllegalArgumentException("Error: More class levels found (" + arrLevels.length
                                               + ") than anticipated (28). The class definitions need to be updated.");
        }

        return arrLevels;
    }

    /**
     * Get the set of minions from a page.
     *
     * @param doc the lodestone profile page to parse.
     * @return the set of strings representing the player's minions.
     */
    private List<String> getMinionsFromPage(final Document doc) {

        // Initialize array in which to store minions
        List<String> minions = new ArrayList<>();
        // Get minion box element
        Elements minionBoxes = doc.getElementsByClass(LAYOUT_CHARACTER_MINION);
        if(!minionBoxes.isEmpty()) {
            // Get minions
            Elements minionSet = minionBoxes.get(0).getElementsByTag(TAG_LI);
            for(int index = 0; index < minionSet.size(); index++) { // For each minion link store into array
                minions.add(minionSet.get(index).getElementsByTag(TAG_DIV).attr(LAYOUT_DATA_TOOLTIP));
            }
        }
        return minions;
    }

    /**
     * Get the set of mounts from a page.
     *
     * @param doc the lodestone profile page to parse.
     * @return the set of strings representing the player's mounts.
     */
    private List<String> getMountsFromPage(final Document doc) {

        // Initialize array in which to store minions
        List<String> mounts = new ArrayList<>();

        // Get minion box element
        Elements minionBoxes = doc.getElementsByClass(LAYOUT_CHARACTER_MOUNTS);
        // Get mounts
        if(!minionBoxes.isEmpty()) {
            Elements mountSet = minionBoxes.get(0).getElementsByTag(TAG_LI);
            for(int index = 0; index < mountSet.size(); index++) { // For each mount link store into array
                mounts.add(mountSet.get(index).getElementsByTag(TAG_DIV).attr(LAYOUT_DATA_TOOLTIP));
            }
        }
        return mounts;
    }

    /**
     * Gets the last-modified date of the Character full body image.
     *
     * @param doc the lodestone profile page to parse
     * @return the date on which the full body image was last modified.
     */
    private Date getDateLastUpdatedFromPage(final Document doc, final int id) {
        Date dateLastModified;
        // Get character image URL.
        String imgUrl = doc.getElementsByClass(LAYOUT_CHARACTER_DETAIL_IMAGE).get(0).getElementsByTag(TAG_A).get(0)
                           .getElementsByTag(TAG_IMG)
                           .get(0).attr(ATTR_SRC);
        String strLastModifiedDate = "";

        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.head(imgUrl).asJson();

            strLastModifiedDate = jsonResponse.getHeaders().get(HEADER_LAST_MODIFIED).toString();
        } catch(Exception e) {
            LOG.warn("Setting last-active date to ARR launch date due to an an error loading character " + id
                     + "'s profile image: " + e.getMessage());
            strLastModifiedDate = "[Sat, 24 Aug 2013 00:00:01 GMT]";
        }

        strLastModifiedDate = strLastModifiedDate.replace("[", "");
        strLastModifiedDate = strLastModifiedDate.replace("]", "");
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

        try {
            dateLastModified = dateFormat.parse(strLastModifiedDate);
        } catch(ParseException e) {
            throw new IllegalArgumentException("Could not correctly parse date 'Last-Modified' header from full body image for character id"
                                               + id);
        }
        return dateLastModified;
    }

    /**
     * Sets a Loadestone Page Loader to use.
     * By default, the PlayerBuilder will be initiatsed with a {@link ProductionLodestonePageLoader}.
     * 
     * @param pageLoader the pageLoader to set
     */
    public void setPageLoader(final LodestonePageLoader pageLoader) {
        this.pageLoader = pageLoader;
    }
}
