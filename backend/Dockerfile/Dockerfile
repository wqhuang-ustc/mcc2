FROM centos:7
RUN yum install -y passwd
RUN yum install -y sudo
RUN yum install -y vim
RUN yum install -y curl
RUN yum install -y initscripts && yum clean all
RUN sudo su
RUN adduser teamartizan
RUN echo "artizan" |passwd --stdin teamartizan
RUN usermod -aG wheel teamartizan
RUN curl --silent --location https://rpm.nodesource.com/setup_6.x | bash -
RUN yum -y install nodejs
WORKDIR /src
COPY . /src/
RUN yum install -y ourcareer-1.0.0-SNAPSHOT20170122153241.noarch.rpm
