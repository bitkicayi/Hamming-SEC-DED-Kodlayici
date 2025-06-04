// Gereki kütüphaneler
import java.awt.*;
import javax.swing.*;

public class Hamming extends JFrame {
    // Arayüzde kullanılacak bileşen tanımları
    private JLabel baslikLabel; // Pencere başlığını göstermek için etiket
    private JTextField veriGirisAlani; // Kullanıcının binary veriyi gireceği metin alanı için
    private JButton kodlaButonu, bellegeKaydetButonu; // Kodlama ve belleğe kaydetme  işlemleri için butonlar
    private JPanel hammingKodPaneli; // Hamming kodunun görselleştirileceği panel için
    private JTextField hataPozisyonAlani; // Hata eklenecek pozisyonun girileceği alan için
    private JButton hataEkleButonu, duzeltButonu; // Hata ekleme ve düzeltme için butonlar
    private JButton bellektenOkuButonu, temizleButonu; // Bellekten kod okuma ve silme için butonlar
    private DefaultListModel<String> bellekModel = new DefaultListModel<>(); // Kodları saklamak için liste modeli
    private JList<String> bellekListesi; // Kod geçmişini listelemek için kullanılacak liste
    private int[] aktifKod = null; // Şu anda ekranda gösterilen (aktif) kod dizisi için
    private java.util.Set<Integer> hataliPozisyonlar = new java.util.HashSet<>(); // Hatalı bit pozisyonları tutmak için

    // Kullanıcının girdiği binary veriyi kontrol eden yardımcı fonksiyon
    private boolean girisGecerliMi(String veri) {
        // Kullanıcı veri girmediyse
        if (veri.equals("8, 16 veya 32 bitlik veri giriniz!")) {
            JOptionPane.showMessageDialog(this, "Veri girmediniz!", "Hatalı Girdi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Veri binary değilse
        if (!veri.matches("[01]+")) {
            JOptionPane.showMessageDialog(this, "Sadece 0 ve 1 girilebilir!", "Hatalı Girdi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Bit uzunluğu kontrolü
        if (!(veri.length() == 8 || veri.length() == 16 || veri.length() == 32)) {
            JOptionPane.showMessageDialog(this, "Lütfen 8, 16 veya 32 bitlik veri giriniz!", "Hatalı Uzunluk", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    // Girilen binary veriyi Hamming SEC-DED'e göre kodlayan fonksiyon
    private int[] hesaplaHammingKodu(String veri) {
        int k = veri.length(); // Orijinal veri uzunluğu
        int r = 0; // Gerekli parity bit sayısı
        while (Math.pow(2, r) < k + r + 1) // 2^r >= k + r + 1 koşulunu sağlayan en küçük r'yi bulma
            r++;
        int n = k + r; // Toplam bit sayısı
        int[] kod = new int[n]; // Hamming kodunun tutulacağı dizi
        // Veriyi parity bitleri boş olacak şekilde yerleştirme
        int veriIndex = 0;
        for (int i = 1; i <= n; i++) {
            if ((i & (i - 1)) == 0) {
                // 2'nin kuvvetleri parity bit yerleri
                kod[i - 1] = 0; // Geçici olarak 0 koyma
            } else {
                // Diğer pozisyonlara veri bitlerini sırayla yerleştirme
                kod[i - 1] = veri.charAt(veriIndex++) - '0';
            }
        }
        // Her parity bitini hesaplama ve ilgili pozisyona yazma
        for (int i = 0; i < r; i++) {
            int parityPos = (1 << i); // 2^i şeklinde parity pozisyonu
            int parity = 0;
            for (int j = 1; j <= n; j++) {
                // Parity bitin kapsadığı pozisyonları kontrol etme
                if (((j & parityPos) != 0) && (j != parityPos)) {
                    parity ^= kod[j - 1]; // XOR ile parity değeri hesaplama
                }
            }
            kod[parityPos - 1] = parity; // Hesaplanan parity'yi ilgili yere yaz
        }
        // SEC-DED için ekstra genel parity biti hesaplanması (bütün bitlerin XOR'u)
        int[] secded = new int[n + 1]; // Son bit genel parity
        int overall = 0;
        for (int i = 0; i < n; i++)
            overall ^= kod[i]; // Tüm bitleri XOR'lama
        secded[n] = overall; // Son pozisyona yazma
        // Diğer kod bitlerini aynen kopyalama
        for (int i = 0; i < n; i++)
            secded[i] = kod[i];
        return secded;
    }
    // Kod dizisi içinden orijinal veriyi çıkartma
    private String kodIcindekiVeriyiCoz(int[] kod) {
        StringBuilder veri = new StringBuilder();
        for (int i = 1; i <= kod.length; i++) {
            // 2'nin kuvveti olmayan pozisyonlar veri bitidir (SEC-DED hariç)
            if ((i & (i - 1)) != 0 && i != kod.length) {
                veri.append(kod[i - 1]);
            }
        }
        return veri.toString();
    }
    // Hangi bitlerin parity (eşlik) biti olduğunu belirleyen yardımcı fonksiyon
    private boolean[] parityKonumlariniDondur(int toplamBit) {
        boolean[] parityMi = new boolean[toplamBit]; // Her bit için parity mi değil mi bilgisi
        // Son biti atlayarak parity konumlarını işaretleme
        for (int i = 0; i < parityMi.length - 1; i++) {
            // sadece 2'nin kuvveti olan indekslerde true olur
            if ((i & (i + 1)) == 0)
                parityMi[i] = true;
        }
        // Son bit SEC-DED için genel parity biti (overall parity)
        parityMi[parityMi.length - 1] = true;
        return parityMi;
    }
    // Hamming kodunu kullanıcıya kutucuklar şeklinde gösteren fonksiyon
    private void guncelleHammingKutucuklari(int[] kod, boolean[] parityBits, int hataIndex) {
        hammingKodPaneli.removeAll(); // Eski kutucukları temizleme
        for (int i = 0; i < kod.length; i++) {
            JLabel bitLabel = new JLabel("" + kod[i], SwingConstants.CENTER); // Bitin değerinin yazılması
            bitLabel.setOpaque(true);
            // Arka plan rengini belirlenmesi
            if (hataliPozisyonlar.contains(i)) {
                if (hataliPozisyonlar.size() == 1) {
                    bitLabel.setBackground(Color.RED); // Tek hata
                } else {
                    bitLabel.setBackground(Color.YELLOW); // Çift hata
                }
            }
            else if (parityBits[i]) {
                bitLabel.setBackground(new Color(120, 200, 255)); // Parity bitleri mavi
            } else {
                bitLabel.setBackground(new Color(140, 255, 170)); // Veri bitleri yeşil
            }
            // Görsel özellikler
            bitLabel.setPreferredSize(new Dimension(32, 44));
            bitLabel.setFont(new Font("Lucida Console", Font.BOLD, 20));
            bitLabel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            // Bitlerin üzerine imleç gelince bit türlerini göstermek için
            if (i == kod.length - 1) {
                bitLabel.setToolTipText("SEC-DED Parity (overall parity biti)");
            } else if (parityBits[i]) {
                bitLabel.setToolTipText("Parity biti");
            } else {
                bitLabel.setToolTipText("Veri biti");
            }
            // Bit numarasını gösteren küçük sayılar
            JLabel numaraLabel = new JLabel("" + (i + 1), SwingConstants.CENTER);
            numaraLabel.setPreferredSize(new Dimension(32, 18));
            numaraLabel.setFont(new Font("Lucida Console", Font.PLAIN, 15));
            // Bit ve numarasını dikey olarak gruplayan panel
            JPanel bitPanel = new JPanel();
            bitPanel.setLayout(new BoxLayout(bitPanel, BoxLayout.Y_AXIS));
            bitPanel.setOpaque(false);
            bitPanel.add(bitLabel);
            bitPanel.add(numaraLabel);
            // Ana hamming kod paneline ekleme
            hammingKodPaneli.add(bitPanel);
        }
        hammingKodPaneli.revalidate(); // Yeniden çizim talebi
        hammingKodPaneli.repaint();    // Görsel güncelleme
    }
    // Gönderilen Hamming kodundaki hatanın yerini bulmak için sendrom hesaplama
    private int sendromHesapla(int[] kod) {
        int r = 0;
        // Hangi parity bitleri hesaplanmalı onu bulmak için gerekli parity bit sayısı
        while ((1 << r) < kod.length) // 2^r < toplam bit sayısı (SEC-DED dahil)
            r++;
        int sendrom = 0;
        int n = kod.length - 1; // Son bit SEC-DED parity biti hesaplamaya dahil edilmez
        // Her parity bit pozisyonu için ilgili bitlerle XOR yapılır
        for (int i = 0; i < r; i++) {
            int parityPos = (1 << i); // 2^i şeklinde parity bit pozisyonu
            int parity = 0;
            for (int j = 1; j <= n; j++) {
                if ((j & parityPos) != 0) {
                    parity ^= kod[j - 1]; // XOR ile parity hesaplama
                }
            }
            if (parity != 0)
                sendrom += parityPos; // Eğer parity hatalıysa, sendroma pozisyon değerini ekleme
        }
        return sendrom; // hatalı bitin pozisyonunu döndürme
    }
    public Hamming() {
        // Uygulama temasına dair bazı estetik ayarlar
        UIManager.put("Button.focus", new Color(0, 0, 0, 0)); // Butonlara tıklanınca oluşan mavi çerçeveyi kaldırma için
        UIManager.put("Panel.background", new Color(245, 245, 245)); // Arkaplan rengi
        UIManager.put("TextField.background", Color.WHITE); // Giriş alanlarının rengi
        // Pencere başlığı ve temel ayarlar için
        setTitle("Hamming SEC&DED Kodlayıcı");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Pencere kapanınca uygulama tamamen kapanması için
        setSize(950, 650); // Pencere boyutu
        setLocationRelativeTo(null); // Ortada açılması için
        // Başlık etiketi ayarları
        baslikLabel = new JLabel("Hamming SEC&DED Kodlayıcı");
        baslikLabel.setFont(new Font("Verdana", Font.BOLD, 28));
        baslikLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Ortaya hizalama
        // Veri giriş etiketi ve alanı
        JLabel veriLabel = new JLabel("Binary Veri:");
        veriGirisAlani = new JTextField("8, 16 veya 32 bitlik veri giriniz!", 35); // Başlangıç yazısı
        kodlaButonu = new JButton("Kodla"); // Kodlama işlemi başlatan buton
        bellegeKaydetButonu = new JButton("Belleğe Kaydet"); // Belleğe kaydeten buton
        veriLabel.setFont(new Font("Verdana", Font.PLAIN, 16));
        veriGirisAlani.setFont(new Font("Lucida Console", Font.PLAIN, 16)); // Sabit genişlikte yazı tipi
        // Giriş alanına tıklanırsa yazının silinmesi, çıkınca geri gelmesi için
        veriGirisAlani.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (veriGirisAlani.getText().trim().isEmpty()) {
                    veriGirisAlani.setText("8, 16 veya 32 bitlik veri giriniz!");
                }
            }
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (veriGirisAlani.getText().equals("8, 16 veya 32 bitlik veri giriniz!")) {
                    veriGirisAlani.setText("");
                }
            }
        });
        // Veri giriş bileşenlerini yatay olarak yerleştiren panel
        JPanel veriPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 2));
        veriPanel.setOpaque(false);
        veriPanel.add(veriLabel);
        veriPanel.add(veriGirisAlani);
        veriPanel.add(kodlaButonu);
        veriPanel.add(bellegeKaydetButonu);
        // Hamming kodunu göstermek için kullanılacak panel
        hammingKodPaneli = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 16));
        hammingKodPaneli.setOpaque(true);
        hammingKodPaneli.setBackground(new Color(248, 249, 255)); // Açık mavi arkaplan
        hammingKodPaneli.setPreferredSize(new Dimension(850, 110)); // Sabit genişlik
        // Hata pozisyonu girişi ve hata butonları
        JLabel hataPozLabel = new JLabel("Hata Pozisyonu:");
        hataPozLabel.setFont(new Font("Verdana", Font.PLAIN, 16));
        hataPozisyonAlani = new JTextField(4); // Kısa metin alanı
        hataPozisyonAlani.setFont(new Font("Lucida Console", Font.PLAIN, 16));
        hataEkleButonu = new JButton("Hata Ekle");
        duzeltButonu = new JButton("Düzelt");
        // Hata ile ilgili bileşenleri yatay sıraya dizme
        JPanel hataPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        hataPanel.setOpaque(false);
        hataPanel.add(hataPozLabel);
        hataPanel.add(hataPozisyonAlani);
        hataPanel.add(hataEkleButonu);
        hataPanel.add(duzeltButonu);
        //Bellek paneli
        bellekModel = new DefaultListModel<>();
        bellekListesi = new JList<>(bellekModel);
        bellekListesi.setFont(new Font("Lucida Console", Font.PLAIN, 16));
        bellekListesi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Aynı anda sadece bir satır seçilebilmesi için
        bellekListesi.setVisibleRowCount(8); // Liste yüksekliği
        // Bellekteki bir satıra çift tıklanırsa o veri okunup ekrana yansıtma
        bellekListesi.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    bellektenOkuButonu.doClick(); // Çift tıklamayla buton davranışı tetikleme
                }
            }
        });
        // Bellek listesini kaydırılabilir hale getirme
        JScrollPane bellekScroll = new JScrollPane(bellekListesi);
        bellekScroll.setPreferredSize(new Dimension(850, 150)); // Görsel boyut
        bellekScroll.setBorder(BorderFactory.createEmptyBorder());
        bellektenOkuButonu = new JButton("Bellekten Oku"); // Bellekten okuma butonu
        temizleButonu = new JButton("Bellekten Sil"); // Seçilen hafıza kaydını silen buton
        // Bellek işlemleri için butonları yatayda yerleştiren panel
        JPanel bellekButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bellekButtonPanel.setOpaque(false);
        bellekButtonPanel.add(bellektenOkuButonu);
        bellekButtonPanel.add(Box.createHorizontalStrut(6));
        bellekButtonPanel.add(temizleButonu);
        // Bellek başlığı
        JLabel bellekLabel = new JLabel("Bellek:");
        bellekLabel.setFont(new Font("Verdana", Font.PLAIN, 17));
        bellekLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Bellek paneli: Başlık, liste ve butonları dikeyde gruplayan ana parça
        JPanel bellekPanel = new JPanel();
        bellekPanel.setOpaque(false);
        bellekPanel.setLayout(new BoxLayout(bellekPanel, BoxLayout.Y_AXIS));
        bellekPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bellekPanel.add(Box.createVerticalStrut(2)); // Üst boşluk
        bellekPanel.add(bellekLabel); // "Bellek:" etiketi
        bellekPanel.add(bellekScroll); // Kaydırılabilir liste
        bellekPanel.add(Box.createVerticalStrut(8)); // Liste ile butonlar arası boşluk
        bellekPanel.add(bellekButtonPanel); // Oku ve sil butonları
        // Tüm bileşenleri bir araya toplayan ana panel (arka plan)
        JPanel anaPanel = new JPanel();
        anaPanel.setLayout(new BoxLayout(anaPanel, BoxLayout.Y_AXIS)); // Bileşenleri dikey hizalama
        anaPanel.setBackground(new Color(245, 245, 245)); // Açık gri zemin
        anaPanel.setBorder(BorderFactory.createEmptyBorder(12, 32, 24, 32)); // Kenarlardan boşluk ayarlama
        // Panel sıralaması (üstten alta doğru yerleştirme)
        anaPanel.add(Box.createVerticalStrut(8));
        anaPanel.add(baslikLabel); // Uygulama başlığı
        anaPanel.add(Box.createVerticalStrut(14));
        anaPanel.add(veriPanel); // Giriş paneli
        anaPanel.add(Box.createVerticalStrut(18));
        anaPanel.add(hammingKodPaneli); // Hamming kod görselleştirme alanı
        anaPanel.add(Box.createVerticalStrut(12));
        anaPanel.add(hataPanel); // Hata ekleme paneli
        anaPanel.add(Box.createVerticalStrut(16));
        anaPanel.add(bellekPanel); // Kod hafızası (liste ve butonlar)
        setContentPane(anaPanel); // Tüm içeriği pencereye yerleştirme
        // Kodla Butonu Aksiyonu
        kodlaButonu.addActionListener(e -> {
            String veri = veriGirisAlani.getText().trim(); // Girişten veriyi alma
            if (!girisGecerliMi(veri)) // Geçerli değilse işlem yapma
                return;
            int[] hammingKod = hesaplaHammingKodu(veri); // Hamming SEC-DED kodunu hesaplama
            aktifKod = hammingKod.clone(); // Aktif kod dizisi olarak saklama
            boolean[] parityMi = parityKonumlariniDondur(hammingKod.length); // Parity bitlerini belirleme
            hataliPozisyonlar.clear(); // Önceden hata oluşturmadan kaynaklı renk hatalarını düzeltme
            // Hamming kodunu görsel olarak ekrana çizme
            guncelleHammingKutucuklari(hammingKod, parityMi, -1);
            veriGirisAlani.setText("8, 16 veya 32 bitlik veri giriniz!"); // Başlangıç yazısını getirme
        });
        // Bellekten Oku butonunun Aksiyonları
        bellektenOkuButonu.addActionListener(e -> {
            hataliPozisyonlar.clear(); // Önceki hatalı pozisyonları temizleme
            int index = bellekListesi.getSelectedIndex(); // Seçili satırın indeksini alma
            if (index < 0) {
                // Hiçbir şey seçilmemişse kullanıcıyı uyarmak için
                JOptionPane.showMessageDialog(this, "Bellekten bir kod seçmelisiniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String secili = bellekModel.getElementAt(index); // Seçili satırdaki metni alma
            // Bellekten kod ifadesini almak için
            String[] parcalar = secili.split("\\| Kod: ");
            if (parcalar.length != 2) {
                JOptionPane.showMessageDialog(this, "Kod formatı bozuk!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Kod kısmını temizleme ve sayısal diziye çevirme
            String kodStr = parcalar[1].trim();
            boolean[] parityMi = parityKonumlariniDondur(kodStr.length());
            int[] kodDizi = new int[kodStr.length()];
            for (int i = 0; i < kodStr.length(); i++)
                kodDizi[i] = kodStr.charAt(i) - '0';
            // Bellekten gelen kod hatalıysa hatalı pozisyonu kırmızıya çevirme
            hataliPozisyonlar.clear();
            int sendrom = sendromHesapla(kodDizi);
            int overallParity = 0;
            for (int i = 0; i < kodDizi.length; i++) // Genel parity değeri (SEC-DED biti dahil) hesaplanır
                overallParity ^= kodDizi[i];
            if (sendrom > 0 && overallParity == 1) {
                hataliPozisyonlar.add(sendrom - 1);
            }
            else if ((sendrom > 0 && overallParity == 0) || (sendrom == 0 && overallParity == 1)) {
                // Çift hata durumu (yerini bilemiyoruz ama bozuk olduğunu biliyoruz)
                for (int i = 0; i < kodDizi.length; i++) {
                    hataliPozisyonlar.add(i); // Tüm bitleri sarıya boyama
                }
                JOptionPane.showMessageDialog(this,
                        "Çift hata tespit edildi! Hatalı bitlerin yeri belirlenemedi. Kod düzeltilemez.",
                        "Çift Hata", JOptionPane.WARNING_MESSAGE);
            }
            // Görsel olarak kutucukları güncelleme
            guncelleHammingKutucuklari(kodDizi, parityMi, -1);
            aktifKod = kodDizi.clone(); // Seçilen kodun artık aktif kod olarak saklanması
        });
        // Hata Ekle butonu Aksiyonları
        hataEkleButonu.addActionListener(e -> {
            if (aktifKod == null) {
                JOptionPane.showMessageDialog(this, "Önce bir kod seçmelisiniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int poz = -1;
            try {
                poz = Integer.parseInt(hataPozisyonAlani.getText().trim()); // Kullanıcının girdiği pozisyonu alma
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Geçerli bir bit pozisyonu giriniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (poz < 1 || poz > aktifKod.length) { // Pozisyon dizinin dışındaysa hata mesajı yazdırma
                JOptionPane.showMessageDialog(this, "Pozisyon 1 ile " + aktifKod.length + " arasında olmalı!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Belirtilen bitin değeri terslenmesi (1->0 ya da 0->1)
            aktifKod[poz - 1] = 1 - aktifKod[poz - 1];
            hataliPozisyonlar.add(poz - 1); // Pozisyonun hatalı olarak işaretlenmesi
            boolean[] parityMi = parityKonumlariniDondur(aktifKod.length);
            guncelleHammingKutucuklari(aktifKod, parityMi, -1); // Renkleri güncelleme
        });
        // Düzelt butonu Aksiyonları
        duzeltButonu.addActionListener(e -> {
            if (aktifKod == null) {
                JOptionPane.showMessageDialog(this, "Önce bir kod seçmelisiniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int sendrom = sendromHesapla(aktifKod); // Sendrom değeri tek hatalı bitin yerini gösterir
            int paritySon = 0;
            for (int i = 0; i < aktifKod.length; i++)
                paritySon ^= aktifKod[i]; // Genel parity değeri (SEC-DED)
            boolean[] parityMi = parityKonumlariniDondur(aktifKod.length);
            // Hata yoksa
            if (sendrom == 0 && paritySon == 0) {
                hataliPozisyonlar.clear();
                JOptionPane.showMessageDialog(this, "Kodda hata bulunamadı!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                guncelleHammingKutucuklari(aktifKod, parityMi, -1);
                return;
            }
            /* Çift hata varsa parity'ler birbirini nötrleyebilir, bu durumda sendrom 0 olur.
               Ama SEC-DED biti paritySon'u etkiler; bu yüzden ayırt edilemez. Düzeltilemez. */
            if (sendrom == 0 && paritySon == 1) {
                JOptionPane.showMessageDialog(this, "Sadece SEC-DED parity bitinde (en sağdaki bit) hata var veya çift hata (düzeltilemez)!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                guncelleHammingKutucuklari(aktifKod, parityMi, -1);
                return;
            }
            // Tek bit hatası  (düzeltilir)
            if (sendrom > 0 && paritySon == 1) {
                aktifKod[sendrom - 1] = 1 - aktifKod[sendrom - 1]; // Hatalı bit düzeltilir
                hataliPozisyonlar.clear(); // Kırmızı işaretlemeler kaldırılır
                guncelleHammingKutucuklari(aktifKod, parityMi, -1);
                int index = bellekListesi.getSelectedIndex();
                if (index >= 0) {
                    // Satırda "Kod: ..." geçiyor mu diye ekstra kontrol
                    String eskiSatir = bellekModel.get(index);
                    if (eskiSatir.contains("Kod:")) { // Bu kontrol, gerçekten bellekteki bir satırı mı güncelliyoruz emin olmak içindir
                        String yeniVeri = kodIcindekiVeriyiCoz(aktifKod);
                        StringBuilder yeniKod = new StringBuilder();
                        for (int bit : aktifKod)
                            yeniKod.append(bit);
                        String yeniGosterim = "Veri: " + yeniVeri + " | Kod: " + yeniKod;
                        bellekModel.set(index, yeniGosterim); // Bellekteki satırı güncelleme
                        JOptionPane.showMessageDialog(
                                this,
                                "Hatalı bit " + sendrom + ". pozisyonda bulundu, düzeltildi ve bellek güncellendi.",
                                "Düzeltildi",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        // Düzeltme yapıldı ama bellekle ilgisi yoksa
                        JOptionPane.showMessageDialog(
                                this,
                                "Hatalı bit " + sendrom + ". pozisyonda bulundu ve düzeltildi.",
                                "Düzeltildi",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } else {
                    // Düzeltme yapıldı ama belleğe kayıtlı değilse
                    JOptionPane.showMessageDialog(
                            this,
                            "Hatalı bit " + sendrom + ". pozisyonda bulundu ve düzeltildi.",
                            "Düzeltildi",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
                return;
            }
            /* Çift hata durumunda sendrom değeri anlamlı değildir. Hangi bitler bozuk bilinemez.
               SEC-DED bu durumu tespit eder ama düzeltemez. */
            if ((sendrom > 0 && paritySon == 0) || (sendrom == 0 && paritySon == 1 && aktifKod[aktifKod.length - 1] == 0)) {
                guncelleHammingKutucuklari(aktifKod, parityMi, -1);
                JOptionPane.showMessageDialog(this, "Birden fazla hata (çift bit hatası, düzeltilemez) tespit edildi!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
        });
        // Bellekten Sil butonu Aksiyonları
        temizleButonu.addActionListener(e -> {
            int selectedIndex = bellekListesi.getSelectedIndex(); // Seçili satırı alma
            if (selectedIndex < 0) {
                JOptionPane.showMessageDialog(this, "Önce silmek istediğiniz kodu seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            bellekModel.remove(selectedIndex); // Bellekten silme
            hammingKodPaneli.removeAll(); // Kod kutularını temizleme
            hammingKodPaneli.revalidate();
            hammingKodPaneli.repaint();
            aktifKod = null; // Aktif kodu sıfırlama
        });
        bellegeKaydetButonu.addActionListener(e -> {
            if (aktifKod == null) {
                JOptionPane.showMessageDialog(this, "Önce bir kod üretmelisiniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String veriStr = kodIcindekiVeriyiCoz(aktifKod); // Hatalı da olsa koddan veri çıkarma
            StringBuilder kodStr = new StringBuilder();
            for (int bit : aktifKod)
                kodStr.append(bit);
            String gosterim = "Veri: " + veriStr + " | Kod: " + kodStr;
            bellekModel.addElement(gosterim); // Belleğe ekleme
        });
    }
        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                Hamming frame = new Hamming();   // Arayüzü başlatma
                frame.setVisible(true);          // Ekrana getirme
                frame.requestFocusInWindow();    // Veri giriş alanındaki yazının gözükmesi için
            });
        }}