import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class NightSkyWeather {

    private static final WebClient client = new WebClient(BrowserVersion.getDefault());

    private static final String baseUrl = "https://www.meteoblue.com/en/weather/outdoorsports/seeing/";
    private static final String city = "kassel_germany_2892518";
    private static final String xPath = "/html/body/div[4]/div/div/main/div/div/div[1]/table/tbody";

    private static final int threshold = 10;
    private static final List<String> days = new LinkedList<>();

    public static void main(String[] args) {
        client.getOptions().setCssEnabled(false);
        client.setAjaxController(new NicelyResynchronizingAjaxController());

        try {
            final HtmlPage page = client.getPage(baseUrl + city);
            client.close();

            final HtmlTableBody table = page.getFirstByXPath(xPath);
            final List<HtmlTableRow> rows = table.getRows();

            final List<String> currentDay = new LinkedList<>();
            int currentDayIndex = 0;

            for (final HtmlTableRow row : rows.subList(1, rows.size())) {
                if (!row.getAttribute("class").isEmpty()) {
                    final List<HtmlTableCell> cells = row.getCells().subList(0 ,4);
                    final List<String> stringData = cells.stream()
                            .map(HtmlTableCell::asText)
                            .collect(Collectors.toList());
                    final List<Integer> integerData = stringData.stream()
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());

                    final int currentHour = integerData.get(0);
                    if ((currentHour >= 23 || currentHour <= 3) && integerData.get(1) <= threshold &&
                            integerData.get(2) <= threshold && integerData.get(3) <= threshold) {
                        currentDay.add(String.join(", ", stringData));
                    }

                    if (currentHour >= 4) {
                        switch (currentDayIndex) {
                            case 0 -> days.add("Today the weather is " + (currentDay.isEmpty() ? "not clear" : "clear"));
                            case 1 -> days.add("Tomorrow the weather is " + (currentDay.isEmpty() ? "not clear" : "clear"));
                            default -> days.add("In " + currentDayIndex + " days the weather is " + (currentDay.isEmpty() ? "not clear" : "clear"));
                        }
                        currentDayIndex++;
                        currentDay.clear();
                    }
                }
            }

            displayTray();
        } catch (IOException e) {
            System.out.println("Couldn't get page from `" + baseUrl + city + "`");
        } catch (AWTException e) {
            System.out.println("Something went wrong when displaying the tray notification");
        }
    }

    private static void displayTray() throws IOException, AWTException {
        if (SystemTray.isSupported()) {
            final MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(actionEvent -> System.exit(0));

            final PopupMenu popup = new PopupMenu();
            popup.add(exit);

            final TrayIcon icon = new TrayIcon(ImageIO.read(NightSkyWeather.class.getResourceAsStream("icon.png")));
            icon.setImageAutoSize(true);
            icon.setToolTip("Night Sky Weather");
            icon.setPopupMenu(popup);
            icon.addActionListener(actionEvent -> {
                try {
                    Desktop.getDesktop().browse(new URI(baseUrl + city));
                    System.exit(0);
                } catch (IOException | URISyntaxException e) {
                    System.out.println("Something went wrong when opening the URI");
                }
            });

            final SystemTray tray = SystemTray.getSystemTray();
            tray.add(icon);
            icon.displayMessage("Night Sky Weather", String.join("\n", days), TrayIcon.MessageType.NONE);
        }
    }

}
