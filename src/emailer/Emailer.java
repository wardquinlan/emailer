package emailer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;

public class Emailer {
	private static Log log = LogFactory.getLog(Emailer.class);
	
	static private ApplicationContext ctx;
	
	private List<Attachment> attachments;
	
	private List<String> to;
	
	private String subject;
	
	private Content content;
	
	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public List<String> getTo() {
		return to;
	}

	public void setTo(List<String> to) {
		this.to = to;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	@Autowired
	private JavaMailSender mailSender;
	
	private String from;
	
	
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public void send() {
		try {
			// do preliminary setup
			MimeMessage message = mailSender.createMimeMessage();
			message.addFrom(new Address[] {new InternetAddress(getFrom())});
			InternetAddress[] recipients = new InternetAddress[to.size()];
			for (int i = 0; i < recipients.length; i++) {
				recipients[i] = new InternetAddress(getTo().get(i));
			}
			message.addRecipients(Message.RecipientType.TO, recipients);
			message.setSubject(getSubject());
			
			// do content of the message
			Multipart multiPart = new MimeMultipart();
			InternetHeaders headers = new InternetHeaders();
			headers.addHeader("Content-Type", getContent().getMimeType());
			MimeBodyPart body = new MimeBodyPart(headers, getContent().getContent().getBytes());
			multiPart.addBodyPart(body);

			// do the attachments
			for (Attachment attachment: getAttachments()) {
				File file = new File(attachment.getPath());
				byte[] rgb = new byte[(int) file.length()];
				BufferedInputStream strm = new BufferedInputStream(new FileInputStream(file));
				strm.read(rgb);
				strm.close();
				headers = new InternetHeaders();
				headers.addHeader("Content-Type", attachment.getMimeType());
				MimeBodyPart part = new MimeBodyPart(headers, rgb);
				part.setFileName(file.getName());
				part.setDataHandler(new DataHandler(new FileDataSource(file)));
				multiPart.addBodyPart(part);
			}

			// send it
			message.setContent(multiPart);
			mailSender.send(message);			
		} catch(MessagingException e) {
			log.error("Could not send message", e);
		} catch(IOException e) {
			log.error("Could not read file(s)", e);
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			log.error("cannot load application context");
			log.error("usage: Emailer /path/to/application-context.xml");
			System.exit(1);
		}
		ctx = new FileSystemXmlApplicationContext("file:" + args[0]);
		Emailer emailer = ctx.getBean("emailer", Emailer.class);
		emailer.send();
	}
}
