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
		createFinalReport();
		dodajTekst("finalReport.html", finalReportString());
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

	private static void createFinalReport(){
		try {  
      		File myObj = new File("finalReport.html");  
      		myObj.delete(); //u slucaju da fajl vec postoji da se obrise i opet kreira
      		if (myObj.createNewFile()) {  
        	System.out.println("File created: " + myObj.getName());  
      	} else {  
        	System.out.println("File " + myObj.getName() + " already exists.");  
      	}  
    	} catch (IOException e) {
      		System.out.println("An error occurred.");
      		e.printStackTrace();  
    	} 
	}

	private static String finalReportString(){
		String html = "<!DOCTYPE html" + 
					  "PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
					  "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
					   "<head>" +
					      "<title>SpotBugs Report</title>" +
					      "<style type=\"text/css\">" +
					    ".tablerow0 {" + 
					      "background: #EEEEEE;" + 
					    "}" + 

					    ".tablerow1 {" + 
					      "background: white;" +
					    "}" +

					    ".detailrow0 {" + 
					      "background: #EEEEEE;" + 
					    "}" + 

					    ".detailrow1 {" + 
					      "background: white;" + 
					    "}" +

					    ".tableheader {" +
					      "background: #b9b9fe;" + 
					      "font-size: larger;" +
					    "}" +

					    ".tablerow0:hover, .tablerow1:hover {" +
					      "background: #aaffaa;" +
					    "}" + 

					    ".priority-1 {" +
					        "color: red;" +
					        "font-weight: bold;" + 
					    "}" +
					    ".priority-2 {" + 
					        "color: orange;" +
					        "font-weight: bold;" +
					    "}" +
					    ".priority-3 {" +
					        "color: green;" +
					        "font-weight: bold;" +
					    "}" +
					    ".priority-4 {" +
					        "color: blue;" +
					        "font-weight: bold;" +
					    "}" +
					    "</style>" +
					      "<script type=\"text/javascript\">" +
					      "function toggleRow(elid) {" +
					        "if (document.getElementById) {" +
					          "element = document.getElementById(elid);" +
					          "if (element) {" +
					            "if (element.style.display == 'none') {" +
					              "element.style.display = 'block';" +
					              //window.status = 'Toggle on!';
					            "} else {" +
					              "element.style.display = 'none';" +
					              //window.status = 'Toggle off!';
					            "}" +
					          "}" +
					        "}" +
					      "}" +
					    "</script>" +
					   "</head>" +
					   "<body>" +
					      "<h1>" +
					         "<a href=\"https://spotbugs.github.io/\">SpotBugs</a> Report" +
					      "<h3> >> for multiple Android Studio projects << </h3>" +
					      "</h1>" + 
					      "<h2>Basic Information</h2>" + 
					      "<p>Project: " + 
					    "project ':app' (main)</p>" +
					      "<p>SpotBugs version: 4.0.1</p>" + 
					      "<br>" +
					      "<h3>All tested projects: </h3>" + 
					      "<p>";
					      for(int i = 0; i < projekti.size(); i++){
					      	html += "<p><span class=\"tableheader\">" + projekti.get(i); 
					        html += "<br>";
					        html += "</span></p>";
					        html += "<a href=\"file://" + projekti.get(i) + "/app/build/SpotBugsReports/main.html\">See full report...</a>";
					      }
					      html += "</p>" +
					   "</body>" + 
					"</html>";

	return html;
	}

}