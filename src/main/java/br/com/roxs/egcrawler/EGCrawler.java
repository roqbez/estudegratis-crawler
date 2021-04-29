package br.com.roxs.egcrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EGCrawler {

	private static final Logger logger = Logger.getLogger(EGCrawler.class);

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static final String URL = "https://www.estudegratis.com.br";

	private static final File CACHE_DIR = new File("cache");

	private static final String ALTERNATIVA_A = "QQ==";
	private static final String ALTERNATIVA_B = "Qg==";
	private static final String ALTERNATIVA_C = "Qw==";
	private static final String ALTERNATIVA_D = "RA==";
	private static final String ALTERNATIVA_E = "RQ==";

	private static final String[] ALTERNATIVAS = new String[] { //
			ALTERNATIVA_A, //
			ALTERNATIVA_B, //
			ALTERNATIVA_C, //
			ALTERNATIVA_D, //
			ALTERNATIVA_E //
	};

	private static final Pattern PATTERN_RESPOSTA_CORRETA = Pattern.compile("Resposta correta: \\<b\\>(.+?)");

	public static void main(String[] args) throws Exception {

		try {

			CACHE_DIR.mkdirs();

			Properties appProps = new Properties();

			try (InputStream in = EGCrawler.class.getResourceAsStream("/application.properties")) {
				appProps.load(in);
			}

			HttpContext context = Util.createHttpClientContext();

			Map<String, String> params = new HashMap<String, String>();
			params.put("login", appProps.getProperty("estudegratis.account.username"));
			params.put("senha", appProps.getProperty("estudegratis.account.password"));

			String resp = Util.post(URL + "/exe/login", params, context);

			if (!resp.contains("\"success\""))
				throw new IllegalArgumentException("Falha ao efetuar o login");

			List<Questao> todasQuestoes = new LinkedList<Questao>();

			boolean stop = false;

			JTextField link = new JTextField(11);
			int action = JOptionPane.showConfirmDialog(null, link, "Digite o link do site:", JOptionPane.OK_CANCEL_OPTION);

			if (action < 0 || Util.isBlank(link.getText()))
				return;

			String materiaUrl = extractMateriaUrl(link.getText(), true);

			if (materiaUrl == null) {
				JOptionPane.showMessageDialog(null, "Endereço inválido", "EGCrawler", JOptionPane.ERROR_MESSAGE);
				return;
			}

			File outputFile = new File("questoes-" + materiaUrl.replace("/", "-") + ".html");

			while (!stop) {

				String page = getPage(materiaUrl, context);

				Document doc = Jsoup.parse(page);

				Elements questoes = doc.select(".questao-de-concurso");

				for (Element element : questoes) {
					Questao questao = parseQuestao(element);
					todasQuestoes.add(questao);
					// System.out.println(questao.getTexto());
				}

				Elements nextLink = doc.select("head link[rel='next']");

				if (!nextLink.isEmpty()) {
					String s = nextLink.first().attr("href");
					materiaUrl = extractMateriaUrl(s, false);
				} else {
					stop = true;
				}
			}

			if (todasQuestoes.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Não foi possível extrair nenhuma questão", "EGCrawler", JOptionPane.ERROR_MESSAGE);
				return;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("<html><head><meta charset=\"UTF-8\"></head><body>");

			StringBuilder sbResp = new StringBuilder();

			int i = 1;

			for (Questao questao : todasQuestoes) {
				obterResposta(questao, context);
				String html = questao.getHtml();

				html = html.replaceAll("class=\"questao-cabecalho\">", "class=\"questao-cabecalho\"> " + i + " - ");

				sbResp.append(i + ":" + questao.getResposta() + " ");

				sb.append(html + "<hr/>\n");

				i++;
			}

			sb.append("<br><br>Respostas: " + sbResp);
			sb.append("</body></html>");

			try (OutputStream out = new FileOutputStream(outputFile)) {
				IOUtils.write(sb.toString(), out, CHARSET);
			}

			JOptionPane.showMessageDialog(null, "Foram obtidas um total de " + todasQuestoes.size() + " questões", "EGCrawler",
					JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JOptionPane.showMessageDialog(null, "Erro: " + e.getMessage(), "EGCrawler", JOptionPane.ERROR_MESSAGE);
			return;
		}

	}

	private static String extractMateriaUrl(String s, boolean firstPage) {
		int idx = s.indexOf("materia/");
		if (idx > 0) {
			s = s.substring(idx + 8);

			if (firstPage) {
				int idx2 = s.lastIndexOf('/');
				if (idx2 > 0 && idx2 != s.length() - 1) {
					String p = s.substring(idx2 + 1);
					if (NumberUtils.isDigits(p)) {
						s = s.substring(0, idx2);
					}
				}
			}

			return s;
		} else
			return null;
	}

	private static String getPage(String materiaUrl, HttpContext context) throws Exception {

		String page;

		File cache = new File(CACHE_DIR, "page-" + materiaUrl.replace("/", "-") + ".html");

		if (cache.exists()) {

			try (InputStream in = new FileInputStream(cache)) {
				page = IOUtils.toString(in, CHARSET);
			}
		} else {
			page = Util.get(URL + "/questoes-de-concurso/materia/" + materiaUrl, context);

			try (OutputStream out = new FileOutputStream(cache)) {
				IOUtils.write(page, out, CHARSET);
			}

		}
		return page;
	}

	private static Questao parseQuestao(Element element) {

		Questao questao = new Questao();

		questao.setId(element.attr("id"));

		Element cabecalho = element.select(".questao-cabecalho").first();

		// TODO: melhorar parsing
		Elements materiaAssuntoAnoBancaConcurso = cabecalho.select("a");
		questao.setMateria(materiaAssuntoAnoBancaConcurso.get(0).text());
		questao.setAssunto(materiaAssuntoAnoBancaConcurso.get(1).text());
		questao.setAno(Integer.valueOf(materiaAssuntoAnoBancaConcurso.get(2).text()));
		questao.setBanca(materiaAssuntoAnoBancaConcurso.get(3).text());

		if (materiaAssuntoAnoBancaConcurso.size() >= 5)
			questao.setConcurso(materiaAssuntoAnoBancaConcurso.get(4).text());

		questao.setCargo(cabecalho.text());

		Element enunciado = element.select(".questao-enunciado").first();
		questao.setEnunciado(enunciado.text());

		for (Element alternativa : element.select(".questao-alternativas li")) {
			String letra = alternativa.select("span").first().text();
			String text = alternativa.text();
			questao.getAlternativas().put(letra.trim().substring(0, 1), text.length() > letra.length() ? text.substring(letra.length() + 1) : text);
		}

		for (Element link : element.select("a")) {
			String href = link.attr("href");
			if (href != null && !href.startsWith("http://")) {
				link.attr("href", URL + href);
			}
		}

		for (Element img : element.select("img")) {
			String src = img.attr("src");
			if (src != null && !src.startsWith("http://")) {
				img.attr("src", URL + src);
			}
		}

		element.select(".questao-responda").remove();
		element.select(".questao-opcoes").remove();

		questao.setHtml(element.toString());

		return questao;
	}

	private static void obterResposta(Questao questao, HttpContext httpContext) throws Exception {

		String chute = ALTERNATIVAS[new Random().nextInt(questao.getAlternativas().size())];

		Map<String, String> params = new HashMap<String, String>();
		params.put("q_codquest", questao.getId());
		params.put("q_resposta", chute);

		String page;

		File cache = new File(CACHE_DIR, "resp-" + questao.getId() + ".json");

		if (cache.exists()) {
			try (InputStream in = new FileInputStream(cache)) {
				page = IOUtils.toString(in, CHARSET);
			}
		} else {
			page = Util.post(URL + "/exe/resolver", params, httpContext);

			try (OutputStream out = new FileOutputStream(cache)) {
				IOUtils.write(page, out, CHARSET);
			}
		}

		if (page.contains("\"success\"")) {
			String r = new String(Base64.decodeBase64(chute));
			questao.setResposta(r);
		} else {
			final Matcher matcher = PATTERN_RESPOSTA_CORRETA.matcher(page);
			if (matcher.find()) {
				String r = matcher.group(1);
				questao.setResposta(r);
			}

		}

	}
}
