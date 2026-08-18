# MediCare — Android aplikacija za brigu o lekovima

*(Napomena: sam folder na disku je i dalje `MedicoCare` — samo je naziv aplikacije na telefonu i naziv projekta promenjen u "MediCare".)*

Android projekat (Kotlin + Jetpack Compose + Room).

## Funkcionalnosti

1. **Unos lekova, vitamina i suplemenata u evidenciju** — naziv, kategorija (Lek/Vitamin/Suplement), doza (broj + jedinica mere: mg/g/mcg/ml/i.j./%/mg/ml/kom), oblik leka (unosi se ili bira iz liste — Tableta, Kapsula, Sirup, Prašak, Injekcija, Kapi, Mast/krema, Ostalo — ili se upiše bilo šta custom), napomena. Naziv se može uneti i skeniranjem barkoda sa kutije (kamera, ML Kit, radi bez interneta). Polja za jedinicu mere i oblik su editabilni padajući meniji sa pretragom dok kucaš — meni se otvara tek posle kratke pauze u kucanju (ne na svaki taster), da brisanje više slova zaredom ne bude isprekidano.
2. **Podešavanje učestalosti uzimanja** — svaki dan, samo određeni dani u nedelji, ili na svakih X sati. Svako pojedinačno vreme može imati svoju dozu ako se razlikuje od podrazumevane (npr. 1 tableta ujutru/popodne, a 0.5 uveče).
3. **Alarmi za uzimanje** — sistemska notifikacija tačno na vreme, dugmad "Uzeto"/"Preskoči", automatsko ponovno zakazivanje posle restarta telefona.
4. **Istorija uzimanja** — svaki alarm upisuje zapis (na čekanju → uzeto/preskočeno), pregled u hronološkom redosledu, pretraga po opsegu datuma (od/do) i sortiranje, ručno označavanje ako korisnik nije reagovao na notifikaciju.
5. **Zakazani pregledi** — unos termina kod lekara (naziv, vrsta pregleda — Opšti, Specijalistički, Kontrolni, Laboratorija, Ultrazvuk, RTG, Stomatolog, Vakcinacija, Ostalo/custom — ustanova, adresa, datum/vreme, napomena), podsetnik pre termina — brzi izbor (30 min/1h/3h/1 dan) ili proizvoljan broj + jedinica (minuti/sati/dani/nedelje/mesec), dugme "Navigacija" koje otvara mape do adrese ustanove.
6. **Izveštaji i analize** — hronološki registar slikanih dokumenata (npr. laboratorijski nalazi): slikanje kamerom ili uvoz iz galerije, naziv i napomena, pregled uvećane slike, pretraga po opsegu datuma i sortiranje.
7. **Pritisak i šećer u krvi** — unos merenja (sistolni/dijastolni pritisak + puls, ili šećer sa jedinicom mmol/L ili mg/dL), filter po vrsti, hronološki pregled. Nije medicinski savet.
8. **Ciklus i plodni dani** — unos početka/kraja menstrualnog ciklusa, okvirna procena narednog ciklusa i plodnog perioda na osnovu proseka prethodnih unosa (potrebna bar 2 zabeležena ciklusa). Ovo je samo procena, ne medicinski savet niti pouzdana kontracepcija.
9. **Skeniranje barkoda na glavnoj listi** — skeniranjem sa glavnog ekrana lista lekova/alarma se filtrira samo na lek povezan sa tim barkodom (dugme za uklanjanje filtera vraća pun prikaz). Ako skenirani barkod ne odgovara nijednom leku u evidenciji, filter se sam uklanja (uz kratku poruku) — ne ostaje "zaglavljen" aktivan nad praznom listom.
10. **Podsetnik za dopunu leka** — nezavisno od alarma za dozu, može se uključiti periodičan podsetnik (interval u danima) da je vreme dopuniti/podići lek u apoteci; datum sledećeg podsetnika se može i ručno izabrati preko birača datuma (podrazumevano je "danas + interval", ali se može promeniti — npr. da ne padne na dan kad znaš da nećeš biti kod kuće). Ako se izabere datum u prošlosti, aplikacija ga sama pomeri unapred za interval dok ne bude budući, da podsetnik sigurno bude zakazan.
11. **Izbor jezika** — 15 jezika (srpski, engleski, makedonski, hrvatski, mađarski, ruski, nemački, italijanski, francuski, španski, švedski, rumunski, portugalski, arapski, turski), bira se u Podešavanjima (dugme "Sačuvaj izmene") ili brzo sa glavnog ekrana (oznaka jezika u vrhu, npr. "SR"/"EN"/"MK") — bez restarta aplikacije.
12. **Hitni brojevi (brzo biranje)** — poseban ekran (ikonica slušalice u vrhu glavne liste) sa 4 osnovna broja: policija, hitna pomoć, vatrogasci, pomoć na putu. Podrazumevano se popune prema jeziku aplikacije, ali se svaki broj može ručno izmeniti (npr. ako si stranac u drugoj državi). Dodir na broj otvara birač telefona (spreman za poziv, jedan dodatni dodir za samo pozivanje — bez automatskog zvanja i bez potrebe za dozvolom za pozivanje). Dodatni, custom brojevi preko osnovna 4 su Premium, a pri dodavanju se bira i jedna od 15 ikonica (kuća, zvezda, sunce, auto, zgrada, bicikl, srce, alat, ljubimac, cvet, šareno, suncobran, torta, brod, nota) da se lakše vizuelno razlikuju u listi.
13. **Navigacija do pregleda** — dugme "Navigacija" (na listi pregleda i u dugmetu na notifikaciji podsetnika) otvara Google Maps direktno u modu "Directions" (uputstva/navigacija), sa trenutnom lokacijom korisnika kao polaznom tačkom — ne samo pribadaču na mapi. Premium funkcija.
14. **Resetuj podešavanja** — dugme u Podešavanjima koje vraća jezik, skin i režim (svetla/tamna) na podrazumevane vrednosti, bez diranja lekova, istorije ili bilo kojih drugih korisničkih podataka.
15. **Uputstvo (Tutorial)** — dugme "?" na glavnom ekranu, odmah pored izbora jezika, otvara ekran sa spiskom svih delova aplikacije (glavni ekran, dodavanje/izmena leka, istorija, pregledi, izveštaji, pritisak/šećer, ciklus, hitni brojevi, podešavanja, Premium/Premium+) — svaka stavka se širi dodirom i objašnjava šta ta funkcija radi i šta je od toga Premium.

## Premium funkcije (za sada bez pravog plaćanja)

- **Skinovi** — nekoliko tema u boji, svaka i sa svojom malom ilustracijom (ne samo boja): Podrazumevana — siva, neutralna, bez simbola (besplatna), Šuma (zelena) — šumica sa rečicom, Nebo (plava) — oblaci, sunce i ptice, Lala (roze) — polje lala, Leto (žuta) — plaža sa suncobranom, ležaljkom, suncem, galebovima i talasima, Lavanda (ljubičasta) — polje lavande, Narandža (narandžasta) — grana sa narandžama, Crna — zvezdana noć sa mesecom, zidićem i mačkom (sve ovo Premium). Ilustracija se vidi i u Podešavanjima pri izboru i kao traka na glavnom ekranu.
- **Prilagođeno (Custom) — Premium+** — sopstvena pozadinska slika (iz galerije), boja akcenta i boja pozadine (biraju se iz ponuđene palete), veličina i stil fonta (podrazumevani/serif/bez serifa/monospace/rukopisni). Za razliku od ostalih skinova (koji su Premium), Custom je deo **Premium+** paketa.
- **Praćenje zalihe leka** — unosiš koliko jedinica imaš, aplikacija automatski oduzima pri svakom "Uzeto" i upozorava kad zaliha padne ispod praga.
- **Deljenje i preuzimanje izveštaja** — sve deljenje/preuzimanje u aplikaciji je Premium: istorija uzimanja (PDF/CSV), izveštaji i analize (deljenje pojedinačne slike ili svih odjednom kao PDF), pritisak i šećer (PDF/CSV), ciklus i plodni dani (PDF/CSV — PDF uključuje vizuelno izdvojenu, drugačije obojenu sekciju sa procenom narednog ciklusa). Pritisak/šećer i ciklus/plodni dani imaju i pravo **preuzimanje** (ne samo deljenje) — PDF/CSV se snima direktno u javni folder Preuzimanja na uređaju (MediaStore na API 29+; na starijim verzijama traži jednokratnu dozvolu za skladište).
- **Podsetnik za dopunu leka** — uključivanje periodičnog podsetnika (i podešavanje intervala) je Premium funkcija; sam unos leka i alarmi za dozu ostaju besplatni.
- **Hitni brojevi — dodatni (custom) brojevi** — osnovna 4 broja (policija/hitna pomoć/vatrogasci/pomoć na putu), uključujući njihovu ručnu izmenu, su besplatni; dodavanje bilo kog broja preko te četvorke je Premium.
- **Navigacija do pregleda** — dugme "Navigacija" (na listi pregleda i u notifikaciji podsetnika) je Premium.

Prava prodaja pretplate (Google Play Billing) još nije uključena — potreban je Google Play Developer nalog (jednokratno $25) i objavljena aplikacija da bi billing uopšte mogao da se testira. Za sada, kad god se pokuša deljenje/preuzimanje ili otključavanje skina bez Premium-a, prikazuje se dijalog sa test-dugmetom "Otključaj (test)" koje privremeno otključava sve Premium funkcije radi probe. Kad budeš spreman da platiš Play Developer nalog, taj test-prekidač se lako zamenjuje pravim tokom kupovine — struktura je već postavljena za to.

## Premium+ i banner reklama (AdMob)

**Premium+ je "Premium + još nešto"** — viši nivo koji UKLJUČUJE sve Premium pogodnosti (skinove, deljenje/preuzimanje izveštaja, podsetnik za dopunu, custom hitne brojeve, navigaciju do pregleda) i dodatno uklanja banner reklamu sa dna glavnog ekrana. U kodu su i dalje dva odvojena prekidača (`premiumUnlocked` i `premiumPlusUnlocked` u `SettingsPreferences`), ali `MedicationViewModel` izlaže izvedeni `hasPremiumAccess` (= Premium ILI Premium+) koji svi ekrani koriste za otključavanje Premium sadržaja — tako Premium+ automatski "nasleđuje" sve iz Premium-a. Jedini izuzetak ide u suprotnom smeru: **Prilagođeni (Custom) skin** je zaključan specifično iza Premium+ (`premiumPlusActive`), ne otključava ga običan Premium.

Premium+ se dobija na dva načina:
1. **Test-otključavanje** (dugme "Otključaj (test)" u Podešavanjima ili u dijalogu) — trajno, dok se ne ugasi ručno.
2. **Rewarded reklama** — dugme "Pogledaj reklamu (1h Premium+)" (na glavnom ekranu iznad bannera i u Podešavanjima). Gledanjem reklame do kraja korisnik dobija Premium+ na 1h; gledanje više puta nadovezuje dodatnih 1h na postojeći rok. Nagrada se dodeljuje isključivo kroz AdMob-ov `onUserEarnedReward` callback (`RewardedAdManager.kt`), tako da se ne može "prevariti" prekidom reklame.

Banner (standardna veličina 320×50, `BannerAdView.kt`) se prikazuje u dnu glavnog ekrana (lista lekova) samo dok Premium+ nije aktivan (ni trajno ni privremeno preko reklame), sa dugmadima "Pogledaj reklamu (1h)" i "Ukloni reklame (Premium+)" iznad njega. U Podešavanjima se, dok je privremeni Premium+ aktivan, prikazuje i preostalo vreme (npr. "Premium+ aktivan još 0h 42min").

**Koriste se PRAVI ID-jevi iz Vladimirovog AdMob naloga** (aplikacija "MediCare"): App ID `ca-app-pub-2860076775666952~6030813575` u manifestu, banner ad-unit ID `ca-app-pub-2860076775666952/6246701926` u `BannerAdView.kt`, rewarded ad-unit ID `ca-app-pub-2860076775666952/7600146197` u `RewardedAdManager.kt`. To znači da se od sada prikazuju **stvarne reklame**, ne Google-ove test reklame.

**Da ne bi došlo do "invalid traffic" upozorenja na nalogu** (AdMob politika zabranjuje česta klikanja/gledanja sopstvenih reklama tokom razvoja): dodaj svaki uređaj na kom testiraš kao **Test device** u AdMob-u (Settings → Test devices → unese se Advertising ID uređaja, dobija se iz Logcat-a pri prvom pokretanju aplikacije — SDK ispiše poruku "Use RequestConfiguration.Builder().setTestDeviceIds(...)" sa tačnim ID-jem tog uređaja). Dok se uređaj ne doda kao test device, izbegavaj ponovljeno gledanje/klikanje na baner i rewarded reklamu.

AdMob nalog trenutno javlja "Payment setup incomplete" — ne sprečava prikazivanje reklama niti razvoj, ali profil plaćanja mora biti dovršen pre nego što aplikacija stvarno počne da isplaćuje prihod i pre objavljivanja na Play Store.

Zavisnost: `com.google.android.gms:play-services-ads`, inicijalizacija u `MainActivity.onCreate()` (`MobileAds.initialize(this)`).

## Skeniranje barkoda — kako radi

Ne postoji javno dostupna baza barkodova za lekove u Srbiji, pa aplikacija sama "uči": prvi put kad se skenira nepoznat barkod, korisnik ručno unese podatke kao i inače, a aplikacija to zapamti lokalno (bez interneta) i poveže sa tim lekom u evidenciji. Sledeći put kad se skenira isti barkod (bilo pri dodavanju leka, bilo na glavnoj listi radi filtriranja), podaci se automatski prepoznaju. Skeniranje barkoda **nije** dostupno na ekranu zakazanih pregleda.

**Pretraga leka preko interneta po barkodu — nije implementirano.** Proverio sam dostupne opcije: besplatnih, pouzdanih barkod-baza koje pokrivaju srpske/EU lekove praktično nema. Postoji npr. DrugsAPI.com koji tvrdi da pokriva 184 zemlje i ima pretragu po barkodu, ali je plaćen (od ~79$/mesečno, treba API ključ i kartica) i nisam mogao da proverim da li stvarno ima srpske lekove bez pretplate. Opšte barkod-baze (EAN-Search, Barcode Lookup, FreeWebApi) su fokusirane na maloprodajne proizvode, ne na farmaceutske EAN kodove, i takođe traže nalog/ključ. Zato je zadržan lokalni pristup "učenja" opisan iznad — pouzdaniji i besplatan, ali zahteva jedan ručni unos po leku.

**Otvoreno pitanje:** pomenuo si nadjilek.rs kao moguć izvor za pretragu naziva leka po delu imena (polje "term"). Nisam mogao da proverim njihov stvarni pretraživački endpoint jer Claude in Chrome ekstenzija nije bila povezana u ovoj sesiji — probao sam par uobičajenih putanja ručno, bez uspeha (stranica je JS-renderovana). Ako povežeš ekstenziju (ili mi pošalješ URL koji vidiš u devtools → Network kad kucaš u pretragu na sajtu), mogu da proverim da li postoji iskoristiv endpoint. Bitno je i imati na umu da bi to bio nezvaničan/reverse-engineered poziv njihovog privatnog API-ja — nema garancije da će raditi trajno.

## Lokalizacija (15 jezika)

Svi tekstovi u ekranima, notifikacijama i PDF/CSV izveštajima čitaju se iz Android string resursa (`res/values*/strings.xml`), tako da prate jezik izabran u Podešavanjima (`AppCompatDelegate.setApplicationLocales`, sa `autoStoreLocales` da izbor preživi restart). Srpski (`values/`) je podrazumevani/fallback jezik; ostalih 14 jezika (`values-en`, `values-mk`, `values-hr`, `values-hu`, `values-ru`, `values-de`, `values-it`, `values-fr`, `values-es`, `values-sv`, `values-ro`, `values-pt`, `values-ar`, `values-tr`) imaju kompletan, ručno prevden set od 375 stringova + 3 liste (oblici leka, jedinice doze, vrste pregleda) — proveren skriptom da nijedan ključ ne nedostaje ni u jednom jeziku.

Nazivi oblika leka / vrsta pregleda / jedinica doze u padajućim listama prate izabrani jezik (npr. "Tableta" → "Tablet" na engleskom), ali su i dalje slobodna polja za ručni unos — ako promeniš jezik nakon što si već uneo lek na starom jeziku, taj tekst ostaje kakav je unet (ne prevodi se retroaktivno), što je i očekivano ponašanje za slobodan tekst. PDF/CSV izveštaji takođe koriste izabrani jezik za naslove i oznake kolona; sami uneti podaci (imena lekova, napomene) ostaju onakvi kakve ih je korisnik uneo.

**Napomena o tehničkom uzroku bag-a "promena jezika ne radi":** `MainActivity` je ranije nasleđivala `ComponentActivity`, a `AppCompatDelegate.setApplicationLocales` na Androidu starijem od 13 (API<33) menja jezik samo aktivnostima koje su `AppCompatActivity` (na Androidu 13+ radi i bez toga, preko sistemskog `LocaleManager`-a). Popravljeno tako što `MainActivity` sad nasleđuje `AppCompatActivity`, a `Theme.MedicoCare` nasleđuje `Theme.AppCompat.DayNight.NoActionBar` (obavezno za AppCompat aktivnosti).

## Hitni brojevi — podrazumevani brojevi po jeziku

Kad se ekran hitnih brojeva prvi put otvori, automatski se popune 4 osnovna broja prema trenutno izabranom jeziku aplikacije (`DefaultEmergencyNumbers.kt`). Ovo su brojevi koje sam proverio pretragom pre nego što sam ih uneo kao podrazumevane (dat je izvor/logika, ne moje pamćenje, jer je pogrešan hitni broj ozbiljna stvar):

- **Srbija (sr)**: policija 192, hitna pomoć 194, vatrogasci 193, pomoć na putu (AMSS) 1987.
- **Severna Makedonija (mk)**: 192 / 194 / 193.
- **Mađarska (hu)**: 107 / 104 / 105.
- **Rusija (ru)**: 102 / 103 / 101.
- **Nemačka (de)**: policija 110, hitna pomoć i vatrogasci 112.
- **Velika Britanija/engleski (en)**: 999 za sve tri službe.
- **Hrvatska, Italija, Francuska, Španija, Švedska, Rumunija, Portugalija, Turska**: jedinstveni evropski broj 112 za sve tri službe (proverena rasprostranjenost 112 u ovim zemljama).
- **Arapski (ar)**: namerno ostavljeno prazno — arapski jezik pokriva 20+ država sa različitim sistemima brojeva, pa nijedan izbor ne bi bio tačan za većinu korisnika.
- **Pomoć na putu**: osim Srbije, nisam mogao pouzdano da potvrdim jedinstven nacionalni broj za većinu zemalja (često je u pitanju privatni/klupski broj, ne državni), pa je ostavljeno prazno svuda osim Srbije.

Prazno polje se u ekranu prikazuje sa jasnom napomenom da broj nije podrazumevano popunjen i da ga korisnik treba sam uneti. Svi podrazumevani brojevi, popunjeni ili prazni, mogu se ručno izmeniti u bilo kom trenutku (npr. stranac koji koristi aplikaciju na svom jeziku u drugoj državi) — izmena ne zahteva Premium.

## Kako otvoriti projekat

1. Instaliraj [Android Studio](https://developer.android.com/studio) (najnovija stabilna verzija).
2. File → Open → izaberi folder `MedicoCare` (ovaj folder).
3. Android Studio će ponuditi da preuzme Gradle wrapper i sve zavisnosti — dozvoli sinhronizaciju (potrebna je internet konekcija prvi put).
4. Pokreni na emulatoru ili fizičkom telefonu (Run ▶, minimalni Android 8.0 / API 26). Za skeniranje barkoda potreban je fizički telefon ili emulator sa virtuelnom kamerom.

Projekat namerno ne sadrži `gradlew`/`gradlew.bat` binarne fajlove — Android Studio ih sam generiše pri prvom otvaranju.

## Prve dozvole u aplikaciji

- **Notifikacije** (Android 13+) — bez ovoga se alarm neće videti.
- **Precizni alarmi** (Android 12+) — baner na listi lekova sa dugmetom "Podesi dozvolu" ako nedostaje.
- **Kamera** — samo kad se prvi put dodirne ikonica za skeniranje barkoda.

## Struktura projekta

```
app/src/main/java/com/medicocare/app/
├── data/         Room baza: Medication, MedicationSchedule, BarcodeEntry, IntakeLog, Appointment,
│                 LabDocument, VitalReading, CycleEntry, EmergencyNumber, DAO-ovi
├── repository/   MedicationRepository, AppointmentRepository, DocumentRepository, VitalsRepository,
│                 CycleRepository, EmergencyNumberRepository — spajaju bazu, alarme i istoriju
├── alarm/        AlarmScheduler/Receiver za lekove, AppointmentAlarmScheduler/Receiver za preglede, notifikacije
├── report/       ReportGenerator — PDF/CSV izveštaj (bez spoljnih biblioteka), uklj. preuzimanje u Downloads
└── ui/           Compose ekrani: lekovi, raspored/alarm, skener, istorija, pregledi, izveštaji,
                  vitalni znaci, ciklus, podešavanja (uklj. SkinArt ilustracije i Prilagođeni skin),
                  TutorialScreen (Uputstvo — "?" dugme na glavnom ekranu, pored izbora jezika)
    └── ads/      BannerAdView — AdMob banner reklama; RewardedAdManager — Rewarded reklama (1h Premium+)
```

Podešavanja izgleda (skin, tema, jezik) se u Podešavanjima biraju u lokalnom stanju i primenjuju tek na dodir dugmeta "Sačuvaj izmene" — dodirivanje kartica/opcija samo menja prikaz na ekranu dok se ne sačuva. Izbor jezika je dostupan i kao brzi prečac na glavnom ekranu (primenjuje se odmah, bez čekanja na Sačuvaj).

Baza je u ranoj fazi razvoja — umesto pisanja Room migracija za svaku izmenu šeme, koristi se `fallbackToDestructiveMigration()`, što znači da se lokalni podaci brišu kad verzija baze poraste (pri sledećoj većoj izmeni).

## Predlog za dalje (nije još implementirano)

- Pravi Google Play Billing tok kupovine (kad budeš spreman za Play Developer nalog).
- Statistika pridržavanja terapije (% uzetih doza kroz vreme) na osnovu istorije.
- Deljenje evidencije sa članom porodice ili lekarom (zahteva nalog i sinhronizaciju).
- Widget na početnom ekranu sa današnjim lekovima/pregledima.
- Pretraga naziva leka po bazi (zavisi od rešenja pitanja oko nadjilek.rs iznad).

Javi kad želiš da nastavimo na nekoj od ovih tačaka, ili ako nešto od postojećih funkcija treba prilagoditi.
