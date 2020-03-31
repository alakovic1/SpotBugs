import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;

//kreirati putanje.txt file sa putanjama na sve projekte i staviti ga u isti direktorij kao skripta
//SVI PROJEKTI MORAJU IMATI MIN VERZIJU GRADLEA 5, jer SpotBugs nize ne podrzava
//iz tog direktorija gdje su skripta i txt pozvati:
// 1. javac SpotBugsSkripta.java
// 2. java SpotBugsSkripta
//u svaki projekat u tom folderu ce biti testiran SpotBugs i izvjestaj ce biti kreiran
//za svaki projekat ce u konzoli biti ispis da li je BUILD uspjesno zavrsen

public class SpotBugsSkripta {

	public static ArrayList<String> projekti = new ArrayList<String>();

	public static void main(String[] args) {

	    //pozivanje funkcije da se ocitaju sve putanje projekata
	    //NAPOMENA: promijeniti putanju txt fajla ako nije u istom direktoriju kao i skripta
	    citajPutanje("putanje.txt");

	    //pozivanje funkcija za dopisivanje u Gradle i pozivanje spotbugsa za svaki projekat
	    for(int i = 0; i < projekti.size(); i++){
			upisiUGradle(projekti.get(i));
			pozoviSpotbugs(projekti.get(i));
		}

		System.out.println("Izvjestaji za svaki projekat pojedinacno se nalaze u projekat -> app -> build -> SpotBugsReports!!");

	}

	private static void citajPutanje(String putanja){

		//citanja putanja svih projekata iz putanje.txt
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(putanja));
			String line = reader.readLine();
			while (line != null) {
				projekti.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void upisiUGradle(String projekat){

		//dodavanje u build.gradle projekta
		String gradleProjekatPutanja = projekat + "/build.gradle";
		String gradleProjekatTekst = "\nbuildscript {\nrepositories {\nmaven {\nurl \"https://plugins.gradle.org/m2/\"\n}\n}\ndependencies {\nclasspath \"com.github.spotbugs:spotbugs-gradle-plugin:2.0.1\"\n}\n}";

		if(daLiTrebaDodatiSpotBugsUGradle(gradleProjekatPutanja, "classpath \"com.github.spotbugs:spotbugs-gradle-plugin:2.0.1\"") == false){
			dodajTekst(gradleProjekatPutanja, gradleProjekatTekst);
		}

		//dodavanje u build.gradle app
		String gradleAppPutanja = projekat + "/app/build.gradle";
		String gradleAppTekst = "\napply plugin: \"com.github.spotbugs\"\nsourceSets {\nmain {\njava.srcDirs = []\n}\n}\nspotbugs {\ntoolVersion = \'4.0.1\'\nignoreFailures = true\nreportsDir = file(\"$project.buildDir/SpotBugsReports\")\neffort = \"max\"\nreportLevel = \"high\"\n}\ntasks.withType(com.github.spotbugs.SpotBugsTask) {\ndependsOn 'assembleDebug'\nclasses = files(\"$project.buildDir/intermediates/javac\")\nsource = fileTree('src/main/java')\nreports {\nhtml.enabled = true\nxml.enabled = false\n}\n}";

		if(daLiTrebaDodatiSpotBugsUGradle(gradleAppPutanja, "apply plugin: \"com.github.spotbugs\"") == false){
			dodajTekst(gradleAppPutanja, gradleAppTekst);
		}


	}

	public static boolean daLiTrebaDodatiSpotBugsUGradle(String putanja, String linija) {

		//citanje iz build.gradle i ispitivanje da li je vec spotbugs dodan
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(putanja));
			String line = reader.readLine();
			while (line != null) {
				//prekida se ako postoji ova linija jer je bitna
				//ako ona postoji, znaci da je spotbugs dodan u projekat kroz ovu skriptu
				if(line.equals(linija)) return true;
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;

	}

	private static void dodajTekst(String putanja, String tekst) {

		//upisivanje u file
		File file = new File(putanja);
		FileWriter fr = null;
		try {
			fr = new FileWriter(file, true);
			fr.write(tekst);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static void pozoviSpotbugs(String projekat){

		//pozivanje komande
		try{
	        String komanda = projekat + "/gradlew -p " + projekat + " spotbugsMain";
			Process proc = Runtime.getRuntime().exec(komanda);

	        //ispis u konzoli da li je BUILD uspjesan
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = "";
	        while((line = reader.readLine()) != null) {
	            System.out.print(line + "\n");
	        }
			proc.waitFor();  
    	}catch(Exception e){
        	//izuzetak
    	}
    	
	}

}