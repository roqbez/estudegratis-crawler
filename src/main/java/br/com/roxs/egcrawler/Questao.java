package br.com.roxs.egcrawler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Questao {

	private String id;

	private String materia;

	private String assunto;

	private int ano;

	private String banca;

	private String concurso;

	private String cargo;

	private String enunciado;

	private Map<String, String> alternativas = new LinkedHashMap<String, String>();

	private String html;

	private String resposta;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMateria() {
		return materia;
	}

	public String getAssunto() {
		return assunto;
	}

	public void setAssunto(String assunto) {
		this.assunto = assunto;
	}

	public void setMateria(String materia) {
		this.materia = materia;
	}

	public int getAno() {
		return ano;
	}

	public void setAno(int ano) {
		this.ano = ano;
	}

	public String getBanca() {
		return banca;
	}

	public void setBanca(String banca) {
		this.banca = banca;
	}

	public String getConcurso() {
		return concurso;
	}

	public void setConcurso(String concurso) {
		this.concurso = concurso;
	}

	public String getCargo() {
		return cargo;
	}

	public void setCargo(String cargo) {
		this.cargo = cargo;
	}

	public String getEnunciado() {
		return enunciado;
	}

	public void setEnunciado(String enunciado) {
		this.enunciado = enunciado;
	}

	public Map<String, String> getAlternativas() {
		return alternativas;
	}

	public void setAlternativas(Map<String, String> alternativas) {
		this.alternativas = alternativas;
	}

	public String getResposta() {
		return resposta;
	}

	public void setResposta(String resposta) {
		this.resposta = resposta;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	@Override
	public String toString() {
		return "[id=" + id + ", materia=" + materia + ", assunto=" + assunto + ", ano=" + ano + ", banca=" + banca + ", concurso=" + concurso + ", cargo="
				+ cargo + ", enunciado=" + enunciado + ", alternativas=" + alternativas + ", resposta=" + resposta + "]";
	}

	public String getTexto() {

		StringBuilder sb = new StringBuilder();
		sb.append(id + " - " + materia + " - " + assunto);
		sb.append("\nAno: " + ano + " Banca: " + banca + " Concurso: " + concurso + " Cargo: " + cargo);

		sb.append("\n\n" + enunciado + "\n");

		for (Entry<String, String> e : alternativas.entrySet()) {
			sb.append("\n" + e.getKey() + " - " + e.getValue());
		}

		sb.append("\n--------------------------------------------------------\n");
		return sb.toString();
	}

}
