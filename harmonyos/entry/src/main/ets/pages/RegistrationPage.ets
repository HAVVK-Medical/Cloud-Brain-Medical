import router from '@ohos.router';
import { formatDate, getStatusLabel } from '../common/Config';
import type { DepartmentOption, DoctorOption, RegistrationSummary, ScheduleOption } from '../models/ApiModels';
import { ApiClient } from '../services/ApiClient';

@Entry
@Component
struct RegistrationPage {
  @State departments: DepartmentOption[] = [];
  @State doctors: DoctorOption[] = [];
  @State schedules: ScheduleOption[] = [];
  @State registrations: RegistrationSummary[] = [];
  @State selectedDepartmentId: number | null = null;
  @State selectedDoctorId: number | null = null;
  @State selectedScheduleId: number | null = null;
  @State loading: boolean = false;
  @State submitting: boolean = false;
  @State error: string = '';
  @State message: string = '';
  @State displayName: string = '';

  private api?: ApiClient;

  aboutToAppear(): void {
    this.api = new ApiClient(getContext(this));
    this.bootstrap();
  }

  get visibleSchedules(): ScheduleOption[] {
    if (!this.selectedDoctorId) {
      return this.schedules;
    }
    return this.schedules.filter((item) => item.doctorId === this.selectedDoctorId);
  }

  get selectedDepartment(): DepartmentOption | undefined {
    return this.departments.find((item) => item.id === this.selectedDepartmentId);
  }

  get selectedDoctor(): DoctorOption | undefined {
    return this.doctors.find((item) => item.id === this.selectedDoctorId);
  }

  get selectedSchedule(): ScheduleOption | undefined {
    return this.visibleSchedules.find((item) => item.id === this.selectedScheduleId);
  }

  async bootstrap(): Promise<void> {
    if (!this.api) {
      return;
    }
    const session = await this.api.getSession();
    if (!session?.token) {
      router.replaceUrl({ url: 'pages/LoginPage' });
      return;
    }
    this.displayName = session.displayName || session.username;
    await this.refreshAll();
  }

  async refreshAll(): Promise<void> {
    if (!this.api) {
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      const departments = await this.api.listDepartments();
      this.departments = departments;
      if (!this.selectedDepartmentId || !departments.some((item) => item.id === this.selectedDepartmentId)) {
        this.selectedDepartmentId = departments[0]?.id ?? null;
      }
      await this.refreshCatalogForDepartment();
      this.registrations = await this.api.listRegistrations();
    } catch (err) {
      this.error = err instanceof Error ? err.message : '加载挂号数据失败';
    } finally {
      this.loading = false;
    }
  }

  async refreshCatalogForDepartment(): Promise<void> {
    if (!this.api) {
      return;
    }
    const departmentId = this.selectedDepartmentId;
    const doctors = await this.api.listDoctors(departmentId);
    const schedules = await this.api.listSchedules(departmentId);
    this.doctors = doctors;
    this.schedules = schedules;
    if (!this.selectedDoctorId || !doctors.some((item) => item.id === this.selectedDoctorId)) {
      this.selectedDoctorId = doctors[0]?.id ?? null;
    }
    this.syncScheduleSelection();
  }

  syncScheduleSelection(): void {
    const items = this.visibleSchedules;
    if (!this.selectedScheduleId || !items.some((item) => item.id === this.selectedScheduleId)) {
      this.selectedScheduleId = items[0]?.id ?? null;
    }
  }

  async chooseDepartment(id: number): Promise<void> {
    this.selectedDepartmentId = id;
    this.selectedDoctorId = null;
    this.selectedScheduleId = null;
    this.loading = true;
    this.error = '';
    try {
      await this.refreshCatalogForDepartment();
    } catch (err) {
      this.error = err instanceof Error ? err.message : '加载科室号源失败';
    } finally {
      this.loading = false;
    }
  }

  chooseDoctor(id: number): void {
    this.selectedDoctorId = id;
    this.syncScheduleSelection();
  }

  chooseSchedule(id: number): void {
    this.selectedScheduleId = id;
  }

  async submitRegistration(): Promise<void> {
    if (!this.api || !this.selectedScheduleId) {
      this.error = '请先选择可用号源';
      return;
    }
    this.submitting = true;
    this.error = '';
    this.message = '';
    try {
      const registration = await this.api.createRegistration({
        scheduleId: this.selectedScheduleId,
        triageRecordId: null
      });
      this.message = `挂号成功：${registration.departmentName ?? ''} ${registration.doctorName ?? ''}`;
      await this.refreshAll();
    } catch (err) {
      this.error = err instanceof Error ? err.message : '挂号失败';
    } finally {
      this.submitting = false;
    }
  }

  async cancelRegistration(id: number): Promise<void> {
    if (!this.api) {
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      await this.api.cancelRegistration(id, 'patient cancelled from HarmonyOS');
      this.message = '已取消挂号';
      await this.refreshAll();
    } catch (err) {
      this.error = err instanceof Error ? err.message : '取消失败';
    } finally {
      this.loading = false;
    }
  }

  async logout(): Promise<void> {
    await this.api?.logoutLocal();
    router.replaceUrl({ url: 'pages/LoginPage' });
  }

  build() {
    Column() {
      Row() {
        Column({ space: 4 }) {
          Text('患者挂号')
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor('#14213D')
          Text(this.displayName ? `您好，${this.displayName}` : '请选择科室、医生与号源')
            .fontSize(13)
            .fontColor('#687385')
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        Button('退出')
          .type(ButtonType.Capsule)
          .fontSize(13)
          .fontColor('#526070')
          .backgroundColor('#EEF2F7')
          .onClick(() => {
            this.logout();
          })
      }
      .width('100%')
      .padding({ left: 18, right: 18, top: 20, bottom: 14 })
      .backgroundColor('#FFFFFF')

      if (this.error) {
        Text(this.error)
          .fontSize(13)
          .fontColor('#B42318')
          .padding(10)
          .margin({ left: 18, right: 18, bottom: 10 })
          .width('90%')
          .borderRadius(10)
          .backgroundColor('#FEEDEB')
      }

      if (this.message) {
        Text(this.message)
          .fontSize(13)
          .fontColor('#0F8A4B')
          .padding(10)
          .margin({ left: 18, right: 18, bottom: 10 })
          .width('90%')
          .borderRadius(10)
          .backgroundColor('#EAF8F0')
      }

      Scroll() {
        Column({ space: 14 }) {
          this.sectionTitle('我的挂号')
          if (this.registrations.length === 0) {
            this.emptyBox('暂无挂号记录')
          } else {
            ForEach(this.registrations, (item: RegistrationSummary) => {
              this.registrationCard(item)
            }, (item: RegistrationSummary) => String(item.id))
          }

          this.sectionTitle('选择科室')
          this.departmentGrid()

          this.sectionTitle('选择医生')
          if (this.doctors.length === 0) {
            this.emptyBox('该科室暂无可用医生')
          } else {
            ForEach(this.doctors, (item: DoctorOption) => {
              this.doctorCard(item)
            }, (item: DoctorOption) => String(item.id))
          }

          this.sectionTitle('选择号源')
          if (this.visibleSchedules.length === 0) {
            this.emptyBox('暂无可用号源')
          } else {
            ForEach(this.visibleSchedules, (item: ScheduleOption) => {
              this.scheduleCard(item)
            }, (item: ScheduleOption) => String(item.id))
          }

          if (this.selectedSchedule) {
            this.confirmCard()
          }
        }
        .padding({ left: 18, right: 18, bottom: 28 })
      }
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F6F8FB')
  }

  @Builder
  sectionTitle(title: string) {
    Text(title)
      .fontSize(17)
      .fontWeight(FontWeight.Bold)
      .fontColor('#14213D')
      .width('100%')
  }

  @Builder
  emptyBox(text: string) {
    Text(text)
      .fontSize(14)
      .fontColor('#687385')
      .padding(18)
      .width('100%')
      .textAlign(TextAlign.Center)
      .borderRadius(12)
      .backgroundColor('#FFFFFF')
  }

  @Builder
  departmentGrid() {
    Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.Start }) {
      ForEach(this.departments, (item: DepartmentOption) => {
        Button(item.name)
          .height(42)
          .margin({ right: 8, bottom: 8 })
          .padding({ left: 14, right: 14 })
          .borderRadius(12)
          .fontSize(14)
          .fontColor(this.selectedDepartmentId === item.id ? '#FFFFFF' : '#25405F')
          .backgroundColor(this.selectedDepartmentId === item.id ? '#0B7FEA' : '#FFFFFF')
          .onClick(() => {
            this.chooseDepartment(item.id);
          })
      }, (item: DepartmentOption) => String(item.id))
    }
    .width('100%')
  }

  @Builder
  doctorCard(item: DoctorOption) {
    Row() {
      Column({ space: 4 }) {
        Text(item.name)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#14213D')
        Text(`${item.title ?? '医生'} · ${item.specialty ?? '门诊'}`)
          .fontSize(12)
          .fontColor('#687385')
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Text(this.selectedDoctorId === item.id ? '已选' : '选择')
        .fontSize(13)
        .fontColor(this.selectedDoctorId === item.id ? '#0B7FEA' : '#687385')
    }
    .padding(14)
    .width('100%')
    .borderRadius(14)
    .backgroundColor(this.selectedDoctorId === item.id ? '#EAF4FF' : '#FFFFFF')
    .border({ width: 1, color: this.selectedDoctorId === item.id ? '#0B7FEA' : '#FFFFFF' })
    .onClick(() => {
      this.chooseDoctor(item.id);
    })
  }

  @Builder
  scheduleCard(item: ScheduleOption) {
    Row() {
      Column({ space: 4 }) {
        Text(`${formatDate(item.workDate)} ${item.period ?? ''}`)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor('#14213D')
        Text(`${item.doctorName ?? '医生'} · ${item.visitLevel ?? '普通门诊'}`)
          .fontSize(12)
          .fontColor('#687385')
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Text(item.remainingSlots && item.remainingSlots > 0 ? `余 ${item.remainingSlots}` : '约满')
        .fontSize(13)
        .fontColor(item.remainingSlots && item.remainingSlots > 0 ? '#0F8A4B' : '#B42318')
    }
    .padding(14)
    .width('100%')
    .borderRadius(14)
    .backgroundColor(this.selectedScheduleId === item.id ? '#EAF4FF' : '#FFFFFF')
    .border({ width: 1, color: this.selectedScheduleId === item.id ? '#0B7FEA' : '#FFFFFF' })
    .opacity(item.remainingSlots && item.remainingSlots > 0 ? 1 : 0.55)
    .onClick(() => {
      if (item.remainingSlots && item.remainingSlots > 0) {
        this.chooseSchedule(item.id);
      }
    })
  }

  @Builder
  registrationCard(item: RegistrationSummary) {
    Column({ space: 8 }) {
      Row() {
        Text(`${item.departmentName ?? '未记录科室'} · ${item.doctorName ?? '未记录医生'}`)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor('#14213D')
          .layoutWeight(1)
        Text(getStatusLabel(item.status))
          .fontSize(12)
          .fontColor(item.status === 'CANCELLED' ? '#B42318' : '#0B7FEA')
      }
      Text(`${formatDate(item.workDate)} ${item.period ?? ''}`)
        .fontSize(12)
        .fontColor('#687385')

      if (item.status === 'WAITING') {
        Button('取消挂号')
          .height(34)
          .fontSize(13)
          .fontColor('#B42318')
          .backgroundColor('#FEEDEB')
          .onClick(() => {
            this.cancelRegistration(item.id);
          })
      }
    }
    .padding(14)
    .width('100%')
    .borderRadius(14)
    .backgroundColor('#FFFFFF')
  }

  @Builder
  confirmCard() {
    Column({ space: 12 }) {
      Text('挂号确认')
        .fontSize(17)
        .fontWeight(FontWeight.Bold)
        .fontColor('#14213D')
        .width('100%')
      Text(`${this.selectedDepartment?.name ?? ''} · ${this.selectedDoctor?.name ?? ''}`)
        .fontSize(14)
        .fontColor('#25405F')
      Text(`${formatDate(this.selectedSchedule?.workDate)} ${this.selectedSchedule?.period ?? ''}`)
        .fontSize(13)
        .fontColor('#687385')
      Button(this.submitting ? '挂号中...' : '确认挂号')
        .height(48)
        .width('100%')
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .backgroundColor('#0B7FEA')
        .enabled(!this.submitting)
        .onClick(() => {
          this.submitRegistration();
        })
    }
    .padding(16)
    .width('100%')
    .borderRadius(16)
    .backgroundColor('#FFFFFF')
  }
}
