import java.util.*;
import java.io.*;

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
		String html = "<!DOCTYPE html\n" + 
					  "PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
					  "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
					   "<head>\n" +
					    "<title>SpotBugs Final Report</title>\n" +
					    "<style type=\"text/css\">\n" +
						    "body {\n" +
						      "text-align: center;\n" +
							"}\n" + 
						    ".greydiv {\n" + 
						      "background: #EEEEEE;\n" + 
						      "width: 850px;\n" +
						      "margin: 0 auto;\n" +
						      "padding: 10px;" +
						    "}\n" +
						    ".purplename {\n" +
						      "background: #b9b9fe;\n" + 
						      "font-size: larger;\n" +
						    "}\n" +
						    "a.spotbugslink {\n" +
						      "color: #4B0082;\n" +
						      "text-decoration:underline;\n" +
						    "}\n" +
						    "a.link {\n" +
						      "font-size: larger;\n" +
						      "color: #4B0082;\n" +
						      "text-decoration:underline;\n" +
						    "}\n" +
						    "a {\n" +
						      "color: black;\n" +
						      "text-decoration: none;\n" + 
						    "}\n" +
						    "#myBtn {\n" +
							  "display: none;\n" +
							  "position: fixed;\n" +
							  "bottom: 20px;\n" +
							  "right: 30px;\n" +
							  "font-size: 15px;\n" +
							  "border: 1px solid black;\n" +
							  "background-color: #b9b9fe;\n" +
							  "color: black;\n" +
							  "padding: 15px;\n" +
							  "border-radius: 4px;\n" +
							"}\n" +
					    "</style>\n" +
					   "</head>\n" +
					   "<body>\n" +
					   "<div class=\"greydiv\">\n" +
					      "<h1>\n" +
					         "<a href=\"https://spotbugs.github.io/\" class=\"spotbugslink\">SpotBugs</a> Final Report\n" +
					      "<h3> >> for multiple Android Studio projects << </h3>\n" +
					      "</h1>\n" + 
					      "<h2>Basic Information</h2>\n" + 
					      "<p>Project: \n" + 
					    "project ':app' (main)</p>\n" +
					      "<p>SpotBugs version: 4.0.1</p>\n" + 
					      "<br>\n" +
					      "<h3>All tested projects: </h3>\n" + 
					      "<p>\n";
					      if(projekti.size() == 0){
					      	html += "<p><u>No projects!!</u></p>";
					      	html += "<p>Please fill putanje.txt file with paths to projects you want to test.</p>";
					      }
					      for(int i = 0; i < projekti.size(); i++){
					      	html += "<p><span class=\"purplename\">" + projekti.get(i) + "\n";
					        html += "<br>\n";
					        html += "</span>\n</p>\n";
					        html += "<p>"+ getWarnings(projekti.get(i) + "/app/build/SpotBugsReports/main.html").get(0) + "</p>";
					        html += "<p><b>Total warnings: </b>" + getWarnings(projekti.get(i) + "/app/build/SpotBugsReports/main.html").get(1) + "</p>";
					        html += "<p>" + getWarnings(projekti.get(i) + "/app/build/SpotBugsReports/main.html").get(2) + "</p>";
					        html += "<a href=\"file://" + projekti.get(i) + "/app/build/SpotBugsReports/main.html\" class=\"link\">See full report...</a>\n";
					      }
					      html += "</p>\n" +
					   "<button onclick=\"topFunction()\" id=\"myBtn\" title=\"Go to top\">Back to top</button>\n" +
					   "</div>\n" +
					   "<script>\n" +
							"var mybutton = document.getElementById(\"myBtn\");\n" +
							"window.onscroll = function(){\n" +
							"scrollFunction()\n" +
							"};\n" +
							"function scrollFunction(){\n" +
							  "if (document.body.scrollTop > 20 || document.documentElement.scrollTop > 20){\n" +
							    "mybutton.style.display = \"block\";\n" +
							  "}else{\n" +
							    "mybutton.style.display = \"none\";\n" +
							  "}\n" +
							"}\n" +
							"function topFunction(){\n" +
							  "document.body.scrollTop = 0;\n" +
							  "document.documentElement.scrollTop = 0;\n" +
							"}\n" +
						"</script>\n" +
					   "</body>\n" + 
					"</html>\n";
	return html;
	}

	private static ArrayList<String> getWarnings(String path){
		ArrayList<String> list = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			String ukupni = "";
			String ukupno = "";
			String total = "";
			while (line != null) {

				//vraca koliko je linija koda analizirano -- RADIII
				if(line.equals("      <h2>Metrics</h2>")){
					ukupno = reader.readLine() + reader.readLine() + reader.readLine();
				}

				//vraca koliko je ukupno warninga -- RADIII
				if(line.equals("               <b>Total</b>")) {
					line = reader.readLine();
					line = reader.readLine();
					line = reader.readLine();
					total = line;
				}

				//vraca contents -- RADIII
				if(line.equals("      <h2>Contents</h2>")){
					line = reader.readLine();
					while(!line.equals("      <h1>Summary</h1>")){
						if(!line.equals("      <ul>") && !line.equals("      </ul>") && !line.equals("         <li>") && !line.equals("         </li>") && !line.equals("            <a href=\"#Details\">Details</a>")){
							ukupni += "<li>";
							ukupni += line;
							ukupni += "</li>";
						}
						line = reader.readLine();
					}
				}
				line = reader.readLine();
			}
			reader.close();

			list.add(ukupno);
			list.add(total);
			list.add(ukupni);

			return list;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}