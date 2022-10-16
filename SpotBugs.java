import java.util.*;
import java.io.*;
import java.awt.Desktop;

//kreirati paths.txt file sa putanjama na sve projekte i staviti ga u isti direktorij kao skripta (ako već nije kreiran)
//SVI PROJEKTI MORAJU IMATI MIN VERZIJU GRADLEA 5, jer SpotBugs nize ne podrzava
//iz tog direktorija gdje su skripta i txt pozvati:
// 1. javac SpotBugs.java
// 2. java SpotBugs
//u svaki projekat u tom folderu ce biti testiran SpotBugs i izvjestaj ce biti kreiran
//za svaki projekat ce u konzoli biti ispis da li je BUILD uspjesno zavrsen
//nakon zavrsetka skripte otvorice se html file koji predstavlja finalni izvjestaj za sve projekte
//u tom izvjestaju ce pisati broj warningsa, koji su to i koliko je linija analizirano
//te ce biti omogucen pristup originalnim izvjestajima za svaki projekat (ako postoje)

//dodana je i opcija da se kreira csv file koji se moze download-ovati kroz html izvjestaj
//ili mu se moze pristupiti kroz direktorij gdje je ova skripta
//ovo je uradjeno da bi se izvjestaj mogao kroz csv otvoriti u Microsoft Excelu


//NAPOMENA: 
//prije nego pokrenete skriptu provjerite:
// 1. da li je projekat kreiran kao Andorid Studio projekat i build-an barem jednom
// 2. verziju Gradle-a u gradle -> wrapper -> gradle-wrapper.properties
// 3. da li je putanja do Android SDK ispravna u local.properties
//ako je ovo sve ispravno, ne bi trebalo biti problema oko pokretanja i kreiranja izvjestaja

public class SpotBugs {

	public static ArrayList<String> projects = new ArrayList<String>(); //lista svih putanja do projekata
	public static Double totalHPW = 0.00; //Total High Priority Warnings [HPW]
	public static Double totalMPW = 0.00; //Total Medium Priority Warnings [MPW]
	public static Double totalHPWaverage = 0.00; //prosjecan HPW za sve projekte
	public static Double totalMPWaverage = 0.00; //prosjecan MPW za sve projekte
	public static int numberOfHPW = 0; //broj projekata koji imaju HPW
	public static int numberOfMPW = 0; //broj projekata koji imaju MPW

	public static void main(String[] args) {

	    //pozivanje funkcije za citanje svih putanja projekata koji se testiraju
	    //NAPOMENA: promijeniti putanju txt file-a ako nije u istom direktoriju kao i skripta
	    readPaths("paths.txt");

	    //pozivanje funkcija za dopisivanje u Gradle i pozivanje spotbugsa za svaki projekat
	    for(int i = 0; i < projects.size(); i++){
			insertTextInGradle(projects.get(i));
			executeSpotBugsCommand(projects.get(i));
		}

		//ispis lokacije svakog individualnog izvjestaja
		System.out.println("All individual project reports can be located in -> app -> build -> SpotBugsReports!!\n");

		//pozivanje funkcije za kreiranje finalnog izvjestaja
		createFinalReport();

		//dodavanje teksta u kreirani finalni html izvjestaj
		addTextToFile("FinalReport.html", finalReportHTMLString()); //pozvana funkcija koja vraca cijeli string za finalni izvjestaj

		//racunanje prosjecnih HPW i MPW
		for(int i = 0; i < projects.size(); i++){
			countWarnings(projects.get(i) + "/app/build/SpotBugsReports/main.html");
		}
		//racuanje prosjecnog broja warningsa
		totalHPWaverage = Math.round((totalHPW / numberOfHPW)*100.00)/100.00;
		totalMPWaverage = Math.round((totalMPW / numberOfMPW)*100.00)/100.00;
		//uzeti u obzir svi slucajevi, da ne bi doslo do NaN ispisa
		if(numberOfHPW == 0){
			totalHPWaverage = 0.00;
		}
		if(numberOfMPW == 0){
			totalMPWaverage = 0.00;
		}

		//dodavanje teksta u kreirani finalni csv izvjestaj
		addTextToFile("FinalReport.csv", finalReportCSVString()); //pozvana funkcija koja vraca cijeli string za finalni izvjestaj

		//pozivanje funkcije za otvaranje finalnog izvjestaja
		openFinalReport();

	}

	private static void readPaths(String path){

		//citanje putanja svih projekata iz paths.txt
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				projects.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			//izuzetak u slucaju da file ne postoji
			e.printStackTrace();
		}

	}

	private static void insertTextInGradle(String project){

		//dodavanje teksta u file-ove zbog uspostavljanja spotbugsa
		try{
			//dodavanje u build.gradle projekta
			String gradleProjectPath = project + "/build.gradle";
			String gradleProjectText = "\nbuildscript {\nrepositories {\nmaven {\nurl \"https://plugins.gradle.org/m2/\"\n}\n}\ndependencies {\nclasspath \"com.github.spotbugs:spotbugs-gradle-plugin:2.0.1\"\n}\n}";

			//ispitivanje da li je vec dodan spotbugs u projekat
			if(SpotBugsInGradleExists(gradleProjectPath, "classpath \"com.github.spotbugs:spotbugs-gradle-plugin:2.0.1\"") == false){
				addTextToFile(gradleProjectPath, gradleProjectText);
			}

			//dodavanje u build.gradle app
			String gradleAppPath = project + "/app/build.gradle";
			String gradleAppText = "\napply plugin: \"com.github.spotbugs\"\nsourceSets {\nmain {\njava.srcDirs = []\n}\n}\nspotbugs {\ntoolVersion = \'4.0.3\'\nignoreFailures = true\nreportsDir = file(\"$project.buildDir/SpotBugsReports\")\neffort = \"max\"\nreportLevel = \"high\"\n}\ntasks.withType(com.github.spotbugs.SpotBugsTask) {\ndependsOn 'assembleDebug'\nclasses = files(\"$project.buildDir/intermediates/javac\")\nsource = fileTree('src/main/java')\nreports {\nhtml.enabled = true\nxml.enabled = false\n}\n}";

			//ispitivanje da li je vec dodan spotbugs u app
			if(SpotBugsInGradleExists(gradleAppPath, "apply plugin: \"com.github.spotbugs\"") == false){
				addTextToFile(gradleAppPath, gradleAppText);
			}
		} catch (Exception e){
        	//izuzetak u slucaju da neki od file-ova ne postoje
    	}

	}

	public static boolean SpotBugsInGradleExists(String path, String string) {

		//citanje iz build.gradle i ispitivanje da li je vec spotbugs dodan
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				//prekida se ako postoji ova linija jer je bitna
				//ako ona postoji, znaci da je spotbugs dodan u projekat kroz ovu skriptu
				if(line.equals(string)) return true;
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			//izuzetak u slucaju da file ne postoji
			e.printStackTrace();
		}
		return false;

	}

	private static void addTextToFile(String path, String text) {

		//upisivanje u file
		try{
			File file = new File(path);
			FileWriter fr = null;
			try {
				fr = new FileWriter(file, true);
				fr.write(text);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e){
        	//izuzetak u slucaju da file ne postoji
    	}

	}

	private static void executeSpotBugsCommand(String project){

		//pozivanje spotbugs komande
		try{
	        String command = project + "/gradlew -p " + project + " spotbugsMain";
			Process process = Runtime.getRuntime().exec(command);

	        //ispis u konzoli da li je BUILD uspjesan
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
	        while((line = reader.readLine()) != null) {
	            System.out.print(line + "\n");
	        }

			int exit = process.waitFor();

			//ispis da li je pozivanje komande uspjesno
            if(exit == 0){
            	//uspjesno pozivanje komande
            	System.out.println("SUCCESSFULLY EXECUTED COMMAND, see if build is successful!!!\n");
            } else {
            	//doslo do problema pri pozivanju komande
            	System.out.println("\nCouldn't execute this command!!!\n");
            	System.out.println("Try executing ./gradlew spotbugsMain command in that project to see what seems to be the problem.\n");
            	System.out.println("Maybe you didn't change your Android SDK path or your project doesn't use Gradle 5+ version.\n");
            }
    	} catch (Exception e){
        	//izuzetak u slucaju da file ne postoji
    	}
    	
	}

	private static void createFinalReport(){

		//kreiranje file-a za finalni izvjestaj
		try {
			//kreiranje html izvjestaja
      		File myObj = new File("FinalReport.html");  
      		//kreiranje csv izvjestaja
      		File myObj2 = new File("FinalReport.csv");

      		//u slucaju da file vec postoji, obrise se i opet kreira
      		myObj.delete(); 
      		myObj2.delete();

      		//kreiranje i ispitivanje da li je kreiran file
      		//za html
      		if (myObj.createNewFile()) {  
        		System.out.println("File created: " + myObj.getName());  
      		} else {  
        		System.out.println("File " + myObj.getName() + " already exists.");  
      		}  
      		//za csv
      		if (myObj2.createNewFile()) {  
        		System.out.println("File created: " + myObj2.getName());  
      		} else {  
        		System.out.println("File " + myObj2.getName() + " already exists.");  
      		} 
    	} catch (IOException e) {
      		e.printStackTrace();  
    	} 

	}

	private static String finalReportHTMLString(){

		//kreiranje citavog stringa za finalni izvjestaj
		String html = "<!DOCTYPE html\n" + 
					  "PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
					  "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
					   "<head>\n" +
					    "<title>SpotBugs Final Report</title>\n" +

					    //css
					    "<style type=\"text/css\">\n" +
						    "body {\n" +
						      "text-align: center;\n" +
							"}\n" + 
						    ".greydiv {\n" + 
						      "background: #EEEEEE;\n" + 
						      "width: 850px;\n" +
						      "margin: 0 auto;\n" +
						      "padding: 10px;\n" +
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

					   //html
					   "<body>\n" +
					   "<div class=\"greydiv\">\n" +

					   	  //ovaj pocetak html-a je kopiran iz pojedinacnih izvjestaja da budu slicni
					      "<h1>\n" +
					         "<a href=\"https://spotbugs.github.io/\" class=\"spotbugslink\">SpotBugs</a> Final Report\n" +
					      "<h3> >> for multiple Android Studio projects << </h3>\n" +
					      "</h1>\n" + 
					      "<h2>Basic Information</h2>\n" + 
					      "<p>Project: \n" + 
						   "project ':app' (main)</p>\n" +
					      "<p>SpotBugs version: 4.0.3</p>\n" + 
					      "<br>\n" +

					      //dodana mogucnost skidanja csv file-a za otvaranje kroz Excel
					      "<a href=\"FinalReport.csv\" class=\"link\">Download CSV report</a>\n" +
					      "<br>\n" +
					      "<br>\n" +

					      //izlistavanje svih testiranih projekata
					      "<h3>All tested projects: </h3>\n" + 
					      "<p>\n";

					      //u slucaju da nisu upisani projekti u paths
					      if(projects.size() == 0){
					      	html += "<p><u>No projects!!</u></p>\n";
					      	html += "<p>Please fill paths.txt file with paths to projects you want to test.</p>\n";
					      }

					      //popunjavanje body-a sa tekstom procitanim iz pojedinacnih izvjestaja
					      for(int i = 0; i < projects.size(); i++){
					      	//naziv projekta, obojen ljubicastom bojom iz pojedinacnih izvjestaja
					      	html += "<p><span class=\"purplename\">" + projects.get(i) + "\n";
					        html += "<br>\n";
					        html += "</span>\n</p>\n";
						    try{
						    	//koliko je analizirano linija
						        html += "<p>"+ getWarnings(projects.get(i) + "/app/build/SpotBugsReports/main.html").get(0) + "</p>\n";
						        //koliko je ukupno warninga
						        html += "<p><b>Total warnings: </b>" + getWarnings(projects.get(i) + "/app/build/SpotBugsReports/main.html").get(1) + "</p>\n";
						        //koja su sve upozorenja u pitanju
						        html += "<p>" + getWarnings(projects.get(i) + "/app/build/SpotBugsReports/main.html").get(2) + "</p>\n";
						        //link za citav pojedinacni izvjestaj za svaki projekat
						        html += "<a href=\"file://" + projects.get(i) + "/app/build/SpotBugsReports/main.html\" class=\"link\">See full report...</a>\n";
						    } catch (Exception e){
						    	//izuzetak u slucaju da neki od file-ova ne postoje
						    	//ili se izvjestaji iz nekog razloga nisu kreirali
        						html += "<p>SpotBugsReport not found!</p>\n"; //dodaje se ovaj tekst u html da nije prazan prostor
    						}
					      }
					      html += "</p>\n" +

					   //dodan button za vracanje na vrh stranice
					   "<button onclick=\"topFunction()\" id=\"myBtn\" title=\"Go to top\">Back to top</button>\n" +
					   "</div>\n" +

					   //js
					   //samo zbog buttona za vracanje na vrh stranice
					   "<script>\n" +
							"var mybutton = document.getElementById(\"myBtn\");\n" +
							"window.onscroll = function(){\n" +
								"scrollFunction()\n" +
							"};\n" +
							"function scrollFunction(){\n" +
							  //ako je scrollano za vise od 20px button za vracanje na vrh stranice se prikaze
							  "if (document.body.scrollTop > 20 || document.documentElement.scrollTop > 20){\n" +
							  	"mybutton.style.display = \"block\";\n" +
							  "}else{\n" +
							  	//inace, button se ne prikazuje
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

	private static String finalReportCSVString(){

		//kreiranje citavog stringa za finalni izvjestaj
		//zaglavlje
		String csv = " , Project, High Priority Warnings [HPW], Medium Priority Warnings [MPW], HPW/Average HPW, MPW/Average MPW\n";
		
		csv += "\n";

		//slucaj kada nema projekata
		if(projects.size() == 0){
			csv += ", NO PROJECTS!!\n";
			csv += ", Please fill paths.txt file with paths to projects you want to test.\n";
		}

		//dodavanje svih projekata
		for(int i = 0; i < projects.size(); i++){ 
			try{
				//slucaj kada je doslo do greske i spotbugs izvjestaj nije kreiran
				if(getHPW(projects.get(i) + "/app/build/SpotBugsReports/main.html") == -1 || getMPW(projects.get(i) + "/app/build/SpotBugsReports/main.html") == -1){
					csv += "," + projects.get(i) + ", /, /, /, /, SpotBugsReport not found!\n";
				}
				else{
					//dodavanje u string
					csv += " ," + projects.get(i) + ", " + getHPW(projects.get(i) + "/app/build/SpotBugsReports/main.html") + ", " + getMPW(projects.get(i) + "/app/build/SpotBugsReports/main.html") + ", ";
					
					//u slucaju da dodje do dijeljenja sa nulom
					if(totalHPWaverage == 0.00 && totalMPWaverage == 0.00){
						csv += "0, 0\n";
					}
					else if(totalHPWaverage == 0.00 && totalMPWaverage != 0.00){
						csv += "0, " + Math.round((getMPW(projects.get(i) + "/app/build/SpotBugsReports/main.html")/totalMPWaverage)*100.00)/100.00 + "\n";
					}
					else if(totalHPWaverage != 0.00 && totalMPWaverage == 0.00){
						csv += Math.round((getHPW(projects.get(i) + "/app/build/SpotBugsReports/main.html")/totalHPWaverage)*100.00)/100.00 +", 0\n";
					}
					//ako se ne dijeli sa nulom
					else{
						csv += Math.round((getHPW(projects.get(i) + "/app/build/SpotBugsReports/main.html")/totalHPWaverage)*100.00)/100.00 + ", " + Math.round((getMPW(projects.get(i) + "/app/build/SpotBugsReports/main.html")/totalMPWaverage)*100.00)/100.00 + "\n";
					}
				}
			} catch(Exception e){
				//izuzetak u slucaju da neki od file-ova ne postoje
			}
		}

		//u slucaju da nema projekata nema smisla da se ovo ispisuje
		if(projects.size() != 0){
			csv += "\n";
			//ispis ukupnog zbira oba Priority Warningsa
			csv += "Total PW, , " + totalHPW + ", " + totalMPW + "\n";
			//ispis prosjecnog iznosa za oba Priority Warningsa
			csv += "Average PW, ," + totalHPWaverage + ", " + totalMPWaverage;
		}
		return csv;

	}

	private static ArrayList<String> getWarnings(String path){

		//kupljenje informacija iz pojedinacnih izvjestaja za finalni izvjestaj
		ArrayList<String> list = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();

			String contents = "";
			String analyzedLines = "";
			String totalWarnings = "";

			while (line != null) {

				//koliko je linija koda analizirano
				if(line.equals("      <h2>Metrics</h2>")){
					analyzedLines = reader.readLine() + reader.readLine() + reader.readLine();
				}

				//koliko je ukupno warninga
				if(line.equals("               <b>Total</b>")) {
					line = reader.readLine();
					line = reader.readLine();
					line = reader.readLine();
					totalWarnings = line;
				}

				//contents, sva upozorenja za projekat
				if(line.equals("      <h2>Contents</h2>")){
					line = reader.readLine();
					while(!line.equals("      <h1>Summary</h1>")){
						if(!line.equals("      <ul>") && !line.equals("      </ul>") && !line.equals("         <li>") && !line.equals("         </li>") && !line.equals("            <a href=\"#Details\">Details</a>")){
							contents += "<li>";
							contents += line;
							contents += "</li>";
						}
						line = reader.readLine();
					}
				}
				line = reader.readLine();
			}
			reader.close();

			list.add(analyzedLines);
			list.add(totalWarnings);
			list.add(contents);

			return list;

		} catch (IOException e) {
			//izuzetak u slucaju da file ne postoji
			e.printStackTrace();
		}
		return null;

	}

	private static void openFinalReport(){

		//otvaranje file-a za finalni izvjestaj
		try{
	        File file = new File("FinalReport.html");

	        //prvo se provjerava da li je Desktop podrzan na platformi koja se koristi
	        if(!Desktop.isDesktopSupported()){
	            System.out.println("\nDesktop is not supported.\n");
	            System.out.println("This file can't be opened.\n");
	            System.out.println("Please open it manually, it's created in this directory.\n");
	            return;
	        }
	        
	        //otvaranje file-a ako je Desktop podrzan
	        Desktop desktop = Desktop.getDesktop();
	        if(file.exists()) {
	        	desktop.open(file);
	        	System.out.println("\nOpening FinalReport.html...\n");
	        }
    	}
    	catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static Integer getHPW(String path){

		//kupljenje koliko je High Priority Warningsa za svaki projekat
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				if(line.equals("            <td>High Priority Warnings</td>")){
					String warningLine = reader.readLine();
					String[] firstSplit = warningLine.split(">", 2); //prvi split
					String[] secondSplit = firstSplit[1].split("<", 2); //drugi i finalni split
					Integer number = 0;
					try{ 
						number = Integer.parseInt(secondSplit[0]);
					}
					catch (Exception e) {
						//u slucaju da se ne moze parsirati string u int jer se nula ne prikazuje u izvjestaju
						number = 0;
					}
					return number;
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			//izuzetak u slucaju da file ne postoji
			e.printStackTrace();
			return -1;
		}
		return 0;

	}

	public static Integer getMPW(String path){

		//kupljenje koliko je Medium Priority Warningsa za svaki projekat
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				if(line.equals("            <td>Medium Priority Warnings</td>")){
					String warningLine = reader.readLine();
					String[] firstSplit = warningLine.split(">", 2); //prvi split
					String[] secondSplit = firstSplit[1].split("<", 2); //drugi i finalni split
					Integer number = 0;
					try{ 
						number = Integer.parseInt(secondSplit[0]);
					}
					catch (Exception e) {
						//u slucaju da se ne moze parsirati string u int jer se nula ne prikazuje u izvjestaju
						number = 0;
					}
					return number;
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			//izuzetak u slucaju da file ne postoji
			e.printStackTrace();
			return -1;
		}
		return 0;
		
	}

	public static void countWarnings(String path){

		//racunanje Priority Warningsa za sve projekte
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				//HPW
				if(line.equals("            <td>High Priority Warnings</td>")){
					String warningLine = reader.readLine();
					String[] firstSplit = warningLine.split(">", 2); //prvi split
					String[] secondSplit = firstSplit[1].split("<", 2); //drugi i finalni split
					Integer number = 0;
					try{ 
						number = Integer.parseInt(secondSplit[0]);
					}
					catch (Exception e) {
						//u slucaju da se ne moze parsirati string u int jer se nula ne prikazuje u izvjestaju
						number = 0;
					}
					//racunaju se samo projekti koji imaju neki warning
					if(number != 0){
						numberOfHPW++;
						totalHPW += number;
					}
				}
				//MPW
				if(line.equals("            <td>Medium Priority Warnings</td>")){
					String warningLine = reader.readLine();
					String[] firstSplit = warningLine.split(">", 2); //prvi split
					String[] secondSplit = firstSplit[1].split("<", 2); //drugi i finalni split
					Integer number = 0;
					try{ 
						number = Integer.parseInt(secondSplit[0]);
					}
					catch (Exception e) {
						//u slucaju da se ne moze parsirati string u int jer se nula ne prikazuje u izvjestaju
						number = 0;
					}
					//racunaju se samo projekti koji imaju neki warning
					if(number != 0){
						numberOfMPW++;
						totalMPW += number;
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			//izuzetak u slucaju da file ne postoji
			e.printStackTrace();
		}

	}

}