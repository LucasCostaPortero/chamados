package br.com.sistemas.chamados.exception;

/** Lançada quando se tenta mudar o status de um chamado para um valor não permitido (vira HTTP 409). */
public class TransicaoStatusInvalidaException extends RuntimeException {
    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
    }
}
