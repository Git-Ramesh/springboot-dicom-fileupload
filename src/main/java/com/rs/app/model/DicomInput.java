package com.rs.app.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class DicomInput implements Serializable {
	private static final long serialVersionUID = -7428077346794376379L;
	private String name;
	private int age;
	private String address;
}
